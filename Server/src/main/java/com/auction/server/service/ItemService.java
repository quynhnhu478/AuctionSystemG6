package com.auction.server.service;

import com.auction.common.enums.Categories;
import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.ItemResponse;
import com.auction.server.model.Auction;
import com.auction.server.model.item.Art;
import com.auction.server.model.item.Electronics;
import com.auction.server.model.item.Item;
import com.auction.server.model.item.Vehicle;

import com.auction.server.repository.ArtRepository;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidHistoryRepository;
import com.auction.server.repository.ElectronicsRepository;
import com.auction.server.repository.ItemFactory;
import com.auction.server.model.user.User;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.VehicleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import com.auction.server.repository.AutoBidRepository;
@Slf4j
@Service
public class ItemService {
    private static final String UPLOAD_DIR = "uploads/items";
    private final ItemRepository itemRepository;
    private final ArtRepository artRepository;
    private final ElectronicsRepository electronicsRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidHistoryRepository bidHistoryRepository;
    // - Key (String): Là tên của Categories (ví dụ: "ELECTRONICS", "ART").
    // - Value (ItemFactory): Là instance của Factory tương ứng.
    private final Map<String, ItemFactory> itemFactoryRegistry;
    private final AutoBidRepository autoBidRepository;
    @Autowired
    public ItemService(ItemRepository itemRepository,
                       ArtRepository artRepository,
                       ElectronicsRepository electronicsRepository,
                       VehicleRepository vehicleRepository,
                       UserRepository userRepository,
                       AuctionRepository auctionRepository,
                       BidHistoryRepository bidHistoryRepository,
                       AutoBidRepository autoBidRepository,
                       Map<String, ItemFactory> itemFactoryRegistry) {
        this.autoBidRepository = autoBidRepository;
        this.itemRepository = itemRepository;
        this.artRepository = artRepository;
        this.electronicsRepository = electronicsRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.bidHistoryRepository = bidHistoryRepository;
        this.itemFactoryRegistry = itemFactoryRegistry;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public List<ItemResponse> getAllItemResponses() {
        List<Item> items = getAllItems();
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .toList();

        Map<Long, Auction> auctionByItemId = auctionRepository.findByItem_IdIn(itemIds)
                .stream()
                .collect(Collectors.toMap(a -> a.getItem().getId(), a -> a));

        List<Long> auctionIds = auctionByItemId.values()
                .stream()
                .map(Auction::getId)
                .toList();

        Map<Long, Long> bidCountByAuctionId = bidHistoryRepository.countByAuctionIds(auctionIds)
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

            Auction auction = auctionByItemId.get(item.getId());
            if (auction != null) {
                response.setPrice(auction.getCurrentPrice());
                response.setBidCount(bidCountByAuctionId.getOrDefault(auction.getId(), 0L).intValue());
            }

            responses.add(response);
        }

        return responses;
    }

    public Item getItemById(Long id) {  //lấy sản phẩm bằng ID
        return itemRepository.findById(id).orElse(null);
    }

    public ItemResponse addItem(ItemRequest itemRequest, Long sellerId) {   //thêm sản phẩm
        ItemFactory itemFactory = null;
        Item savedItem = null;
        try {
            log.info("Bắt đầu xử lý thêm sản phẩm mới cho người bán có ID: {}", sellerId);
            List<String> base64Images = normalizeIncomingImages(itemRequest);
            List<String> savedFiles = saveImages(base64Images);
            String savedFileName = savedFiles.isEmpty() ? "no-image.jpg" : savedFiles.get(0);

            //lấy thông tin người bán từ database
            User seller = userRepository.findById(sellerId).orElseThrow(() -> new RuntimeException("Không tìm thấy người bán với ID: " + sellerId));
            //lấy category từ request
            Categories category = itemRequest.getCategories();
            if (category == null) {
                throw new IllegalArgumentException("Category is required");
            }
            //tìm factory tương ứng
            itemFactory = itemFactoryRegistry.get(category.name());
            if (itemFactory == null) {
                throw new IllegalArgumentException("Unsupported category: " + category);
            }
            //khởi tạo món hàng mới thông qua factory
            Item item = itemFactory.createItem(itemRequest, savedFileName, seller);
            //lưu món hàng vào database
            savedItem = itemRepository.save(item);
            log.info("Đã lưu sản phẩm mới thành công vào DB - Item ID: {}, Tên: {}", savedItem.getId(), savedItem.getName());

            createAuctionForItem(savedItem);

        } catch (Exception e) {
            log.error("Lỗi xử lý lưu sản phẩm tại Server cho Seller ID {}: {}", sellerId, e.getMessage(), e);
            throw new RuntimeException("Failed to create item: " + e.getMessage(), e);
        }
        if (savedItem == null || itemFactory == null) {
            throw new RuntimeException("Failed to create item");
        }
        //Chuyển đổi Entity thành DTO Response và trả về cho Controller
        return itemFactory.mapToResponse(savedItem);
    }

    public ItemResponse updateItem(Long id, ItemRequest itemRequest) {   //chỉnh sửa thông tin sản phẩm
        ItemFactory itemFactory = null;
        Item savedItem = null;
        try {
            log.info("Nhận yêu cầu cập nhật thông tin cho sản phẩm có ID: {}", id);
            //tìm sản phẩm cũ trong database
            Item existingItem = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            List<String> base64Images = normalizeIncomingImages(itemRequest);
            if (!base64Images.isEmpty()) { //check xem client có gửi ảnh mới không
                deleteExistingImages(existingItem);
                List<String> newFiles = saveImages(base64Images);
                String firstImage = newFiles.isEmpty() ? "no-image.jpg" : newFiles.get(0);
                existingItem.setImageUrl(firstImage);
                log.info("Đã cập nhật lưu {} ảnh mới cho item có mã số ID: {}", newFiles.size(), id);
            }

            //lấy loại sản phẩm
            Categories category = existingItem.getCategories();
            //tìm factory phù hợp
            itemFactory = itemFactoryRegistry.get(category.name());
            //sửa thông tin sản phẩm
            itemFactory.updateItem(existingItem, itemRequest);
            //lưu sản phẩm
            savedItem = itemRepository.save(existingItem);
            log.info("Cập nhật thông tin chi tiết sản phẩm ID: {} thành công.", id);
        } catch (Exception e) {
            // Đã tối ưu cấu trúc log.error, bổ sung tham số ngoại lệ 'e' để hiển thị đầy đủ Stack Trace lỗi
            log.error("Lỗi nghiêm trọng khi thực hiện cập nhật sản phẩm có ID: {}. Chi tiết: {}", id, e.getMessage(), e);
        }

        //trả về
        return (itemFactory != null && savedItem != null) ? itemFactory.mapToResponse(savedItem) : null;
    }

    @Transactional
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        auctionRepository.findByItem_Id(id).ifPresent(auction -> {
            bidHistoryRepository.deleteByAuctionId(auction.getId());
            autoBidRepository.deleteByAuctionId(auction.getId());
            auctionRepository.delete(auction);
        });

        deleteExistingImages(item);
        itemRepository.delete(item);

        log.info("Đã xóa sản phẩm ID: {}", id);
    }

    private void createAuctionForItem(Item item) {
        if (auctionRepository.findByItem_Id(item.getId()).isPresent()) {
            return;
        }
        Auction newAuction = Auction.fromItem(item);
        auctionRepository.save(newAuction);
        log.info("Hệ thống tự động kích hoạt tạo phiên đấu giá mới thành công cho sản phẩm mã số ID: {}", item.getId());
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

    private List<String> saveImages(List<String> base64Images) throws Exception {
        if (base64Images == null || base64Images.isEmpty()) {
            return Collections.emptyList();
        }
        ensureUploadDirExists();
        List<String> savedFiles = new ArrayList<>();
        for (String base64 : base64Images) {
            String fileName = UUID.randomUUID().toString() + ".jpg";
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            File imageFile = new File(UPLOAD_DIR + File.separator + fileName);
            try (OutputStream os = new FileOutputStream(imageFile)) {
                os.write(imageBytes);
            }
            savedFiles.add(fileName);
            log.info("Đã lưu ảnh vật lý tại đường dẫn: {}", imageFile.getAbsolutePath());
        }
        return savedFiles;
    }

    private void ensureUploadDirExists() {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void deleteExistingImages(Item existingItem) {
        deleteImageFile(existingItem.getImageUrl());
    }

    private void deleteImageFile(String imageName) {
        if (imageName == null || imageName.isBlank() || "no-image.jpg".equals(imageName) || "no-image.png".equals(imageName)) {
            return;
        }
        File oldFile = new File(UPLOAD_DIR + File.separator + imageName);
        if (oldFile.exists()) {
            oldFile.delete();
            log.info("Đã tiến hành dọn dẹp, xóa file ảnh vật lý cũ trên Server: {}", imageName);
        }
    }
}