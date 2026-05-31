package com.auction.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.enums.Categories;
import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.ItemResponse;
import com.auction.server.model.Auction;
import com.auction.server.model.item.Item;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.auction.server.repository.ArtRepository;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidHistoryRepository;
import com.auction.server.repository.ElectronicsRepository;
import com.auction.server.model.item.ItemFactory;
import com.auction.server.model.user.User;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.VehicleRepository;
import com.auction.server.util.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import com.auction.server.repository.AutoBidRepository;
import com.auction.common.payload.AuctionUpdateResponse;

@Slf4j
@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final ArtRepository artRepository;
    private final ElectronicsRepository electronicsRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    private final Map<String, ItemFactory> itemFactoryRegistry;
    private final AutoBidRepository autoBidRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    public ItemService(ItemRepository itemRepository,
                       ArtRepository artRepository,
                       ElectronicsRepository electronicsRepository,
                       VehicleRepository vehicleRepository,
                       UserRepository userRepository,
                       AuctionRepository auctionRepository,
                       BidHistoryRepository bidHistoryRepository,
                       FileStorageService fileStorageService,
                       AutoBidRepository autoBidRepository,
                       Map<String, ItemFactory> itemFactoryRegistry,
                       SimpMessagingTemplate simpMessagingTemplate) {
        this.autoBidRepository = autoBidRepository;
        this.itemRepository = itemRepository;
        this.artRepository = artRepository;
        this.electronicsRepository = electronicsRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.bidHistoryRepository = bidHistoryRepository;
        this.fileStorageService = fileStorageService;
        this.itemFactoryRegistry = itemFactoryRegistry;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public List<ItemResponse> getAllItemResponses() {
        return getItemResponses(null, null);
    }

    public List<ItemResponse> getItemResponses(Categories categoryFilter, Long sellerId) {
        List<Item> items;
        if (sellerId != null && categoryFilter != null) {
            items = itemRepository.findBySellerIdAndCategoryName(sellerId, categoryFilter.name());
        } else if (sellerId != null) {
            items = itemRepository.findBySeller_Id(sellerId);
        } else if (categoryFilter != null) {
            items = itemRepository.findByCategoryName(categoryFilter.name());
        } else {
            items = getAllItems();
        }

        List<Long> itemIds = items.stream().map(Item::getId).toList();
        if (itemIds.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Auction> auctionByItemId = auctionRepository.findByItem_IdIn(itemIds)
                .stream()
                .collect(Collectors.toMap(a -> a.getItem().getId(), a -> a));

        List<Long> auctionIds = auctionByItemId.values().stream().map(Auction::getId).toList();

        Map<Long, Long> bidCountByAuctionId = auctionIds.isEmpty()
                ? Collections.emptyMap()
                : bidHistoryRepository.countByAuctionIds(auctionIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        List<ItemResponse> responses = new ArrayList<>();

        for (Item item : items) {
            Categories category = item.getCategories();
            if (category == null) continue;

            ItemFactory factory = itemFactoryRegistry.get(category.name());
            if (factory == null) continue;

            ItemResponse response = factory.mapToResponse(item);
            response.setServerTime(LocalDateTime.now());
            Auction auction = auctionByItemId.get(item.getId());
            if (auction != null) {
                String previousStatus = auction.getStatus();
                syncAuctionStatusByTime(auction);
                if (!Objects.equals(previousStatus, auction.getStatus())) {
                    auctionRepository.save(auction);
                }
                response.setAuctionId(auction.getId());
                response.setAuctionStatus(auction.getStatus());
                if (auction.getSeller() != null) {
                    response.setSellerName(auction.getSeller().getName());
                }
                response.setStartingTime(auction.getStartTime());
                response.setEndTime(auction.getEndTime());
                response.setPrice(auction.getCurrentPrice());
                response.setBidCount(bidCountByAuctionId.getOrDefault(auction.getId(), 0L).intValue());
            }
            responses.add(response);
        }
        return responses;
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id).orElse(null);
    }

    public ItemResponse ItemDetail(Long itemId) {
        Item item = getItemById(itemId);
        if (item == null) {
            throw new RuntimeException("Item not found with ID: " + itemId);
        }
        Categories category = item.getCategories();
        if (category == null) {
            throw new RuntimeException("Item with ID " + itemId + " has no category");
        }
        ItemFactory factory = itemFactoryRegistry.get(category.name());
        if (factory == null) {
            throw new RuntimeException("Unsupported category: " + category);
        }
        ItemResponse response = factory.mapToResponse(item);
        response.setServerTime(LocalDateTime.now());
        Optional<Auction> auction = auctionRepository.findByItem_Id(item.getId());
        if (auction.isPresent()) {
            response.setAuctionId(auction.get().getId());
            response.setAuctionStatus(auction.get().getStatus());
            if (auction.get().getSeller() != null) {
                response.setSellerName(auction.get().getSeller().getName());
            }
            response.setStartingTime(auction.get().getStartTime());
            response.setEndTime(auction.get().getEndTime());
            response.setPrice(auction.get().getCurrentPrice());
            response.setBidCount((int) bidHistoryRepository.countByAuctionId(auction.get().getId()));
        }
        return response;
    }

    public ItemResponse addItem(ItemRequest itemRequest, Long sellerId) {
        ItemFactory itemFactory = null;
        Item savedItem = null;
        try {
            log.info("Bắt đầu xử lý thêm sản phẩm mới cho người bán có ID: {}", sellerId);
            List<String> savedImagePaths = saveIncomingImages(itemRequest);
            String firstImage = savedImagePaths.isEmpty() ? null : savedImagePaths.get(0);

            User seller = userRepository.findById(sellerId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người bán với ID: " + sellerId));

            Categories category = itemRequest.getCategories();
            if (category == null) {
                throw new IllegalArgumentException("Category is required");
            }

            itemFactory = itemFactoryRegistry.get(category.name());
            if (itemFactory == null) {
                throw new IllegalArgumentException("Unsupported category: " + category);
            }

            Item item = itemFactory.createItem(itemRequest, firstImage, seller);
            item.setImageUrls(String.join(",", savedImagePaths));
            savedItem = itemRepository.save(item);
            log.info("Đã lưu sản phẩm mới thành công vào DB - Item ID: {}, Tên: {}", savedItem.getId(), savedItem.getName());

            Auction auction = createAuctionForItem(savedItem);

            ItemResponse response = itemFactory.mapToResponse(savedItem);
            response.setAuctionId(auction.getId());
            response.setAuctionStatus(auction.getStatus());
            response.setStartingTime(auction.getStartTime());
            response.setEndTime(auction.getEndTime());
            response.setPrice(auction.getCurrentPrice());
            response.setBidCount(0);
            response.setServerTime(LocalDateTime.now());

            publishItemEvent("ITEM_CREATED", response);
            return response;

        } catch (Exception e) {
            log.error("Lỗi xử lý lưu sản phẩm tại Server cho Seller ID {}: {}", sellerId, e.getMessage(), e);
            throw new RuntimeException("Failed to create item: " + e.getMessage(), e);
        }
    }

    public ItemResponse updateItem(Long id, ItemRequest itemRequest) {
        ItemFactory itemFactory = null;
        Item savedItem = null;
        try {
            log.info("Nhận yêu cầu cập nhật thông tin cho sản phẩm có ID: {}", id);
            Item existingItem = itemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            Optional<Auction> auctionOpt = auctionRepository.findByItem_Id(id);
            if (auctionOpt.isPresent()) {
                rejectUpdateIfAuctionFinished(auctionOpt.get());
            }

            List<String> savedImagePaths = saveIncomingImages(itemRequest);

            if (!savedImagePaths.isEmpty()) {
                String firstImage = savedImagePaths.get(0);

                existingItem.setImageUrl(firstImage);
                existingItem.setImageUrls(String.join(",", savedImagePaths));
            }

            Categories category = existingItem.getCategories();
            itemFactory = itemFactoryRegistry.get(category.name());
            itemFactory.updateItem(existingItem, itemRequest);

            savedItem = itemRepository.save(existingItem);

            if (auctionOpt.isPresent()) {
                Auction auction = auctionOpt.get();
                auction.setStartTime(savedItem.getStartingTime());
                auction.setEndTime(savedItem.getEndTime());

                LocalDateTime now = LocalDateTime.now();
                if (auction.getStartTime() != null && now.isBefore(auction.getStartTime())) {
                    auction.setStatus("PENDING");
                } else if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
                    auction.setStatus(auction.getWinner() == null ? "CANCELED" : "ENDED");
                } else {
                    auction.setStatus("ACTIVE");
                }

                auctionRepository.save(auction);

                AuctionUpdateResponse update = new AuctionUpdateResponse();
                update.setItemId(savedItem.getId());
                update.setAuctionId(auction.getId());
                update.setCurrentPrice(auction.getCurrentPrice());
                update.setEndTime(auction.getEndTime());
                update.setServerTime(LocalDateTime.now());
                update.setMessage("Auction time updated");

                simpMessagingTemplate.convertAndSend("/topic/auction-" + auction.getId(), update);
            }

            log.info("Cập nhật thông tin chi tiết sản phẩm ID: {} thành công.", id);
        } catch (Exception e) {
            log.error("Lỗi nghiêm trọng khi thực hiện cập nhật sản phẩm có ID: {}. Chi tiết: {}", id, e.getMessage(), e);
        }

        if (itemFactory == null || savedItem == null) {
            return null;
        }

        ItemResponse response = itemFactory.mapToResponse(savedItem);

        // ĐÃ SỬA: Lấy lại thực thể đấu giá an toàn bên ngoài khối ifPresent cũ
        Optional<Auction> finalAuctionOpt = auctionRepository.findByItem_Id(savedItem.getId());
        if (finalAuctionOpt.isPresent()) {
            Auction finalAuction = finalAuctionOpt.get();
            response.setAuctionId(finalAuction.getId());
            response.setAuctionStatus(finalAuction.getStatus());
            response.setStartingTime(finalAuction.getStartTime());
            response.setEndTime(finalAuction.getEndTime());
            response.setPrice(finalAuction.getCurrentPrice());
        }

        response.setBidCount(0);
        response.setServerTime(LocalDateTime.now());

        publishItemEvent("ITEM_UPDATED", response);
        return response;
    }
    private void publishItemEvent(String type, ItemResponse response) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("itemId", response.getId());
        event.put("sellerId", response.getSellerId());
        event.put("categories", response.getCategories() == null ? null : response.getCategories().name());
        event.put("serverTime", LocalDateTime.now().toString());

        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(event);
            simpMessagingTemplate.convertAndSend("/topic/items", json);
        } catch (Exception e) {
            log.error("Cannot publish item socket event", e);
        }
    }
    @Transactional
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No products found"));

        auctionRepository.findByItem_Id(id).ifPresent(auction -> {
            bidHistoryRepository.deleteByAuctionId(auction.getId());
            autoBidRepository.deleteByAuctionId(auction.getId());
            auctionRepository.delete(auction);
        });

        itemRepository.delete(item);

        Map<String, Object> deleted = new HashMap<>();
        deleted.put("type", "ITEM_DELETED");
        deleted.put("itemId", id);
        deleted.put("serverTime", LocalDateTime.now().toString());

        try {
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(deleted);
            simpMessagingTemplate.convertAndSend("/topic/items", json);
        } catch (Exception e) {
            log.error("Cannot publish item delete socket event", e);
        }
    }

    private Auction createAuctionForItem(Item item) {
        return auctionRepository.findByItem_Id(item.getId())
                .orElseGet(() -> {
                    Auction newAuction = Auction.fromItem(item);
                    Auction savedAuction = auctionRepository.save(newAuction);
                    log.info("Created auction for item ID: {}", item.getId());
                    return savedAuction;
                });
    }

    private void syncAuctionStatusByTime(Auction auction) {
        if (AuctionStatus.FINISHED.toString().equals(auction.getStatus()) ||
                AuctionStatus.PAID.toString().equals(auction.getStatus()) ||
                AuctionStatus.CANCELED.toString().equals(auction.getStatus())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        if (auction.getStartTime() != null && now.isBefore(auction.getStartTime())) {
            auction.setStatus(AuctionStatus.OPEN.toString());
        } else if (auction.getEndTime() != null && !now.isBefore(auction.getEndTime())) {
            auction.setStatus(auction.getWinner() == null
                    ? AuctionStatus.CANCELED.toString()
                    : AuctionStatus.FINISHED.toString());
        } else {
            auction.setStatus(auction.getWinner() == null
                    ? AuctionStatus.OPEN.toString()
                    : AuctionStatus.RUNNING.toString());
        }
    }

    private void rejectUpdateIfAuctionFinished(Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        boolean endedByTime = auction.getEndTime() != null && !now.isBefore(auction.getEndTime());
        boolean endedByStatus = "ENDED".equalsIgnoreCase(auction.getStatus())
                || "PAID".equalsIgnoreCase(auction.getStatus())
                || "CANCELED".equalsIgnoreCase(auction.getStatus());

        if (endedByTime || endedByStatus) {
            throw new IllegalArgumentException("Cannot edit item after auction has ended.");
        }
    }

    private List<String> normalizeIncomingImages(ItemRequest itemRequest) {
        List<String> base64Images = new ArrayList<>();
        if (itemRequest.getImageBase64List() != null) {
            for (String image : itemRequest.getImageBase64List()) {
                if (image != null && !image.isBlank()) {
                    base64Images.add(image);
                }
            }
        }
        if (base64Images.isEmpty() && itemRequest.getImageBase64() != null && !itemRequest.getImageBase64().isBlank()) {
            base64Images.add(itemRequest.getImageBase64());
        }
        return base64Images;
    }

    private List<String> saveIncomingImages(ItemRequest itemRequest) {
        List<String> images = normalizeIncomingImages(itemRequest);
        List<String> savedPaths = new ArrayList<>();
        for (String image : images) {
            if (image.startsWith("http") || image.startsWith("/uploads/")) {
                savedPaths.add(image);
            } else {
                savedPaths.add(fileStorageService.saveImage(image, "items"));
            }
        }
        return savedPaths;
    }

}
