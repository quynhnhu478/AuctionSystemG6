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
    private final NotificationService notificationService;
    // - Key (String): Là tên của Categories (ví dụ: "ELECTRONICS", "ART").
    // - Value (ItemFactory): Là instance của Factory tương ứng.
    private final Map<String, ItemFactory> itemFactoryRegistry;
    @Autowired
    public ItemService(ItemRepository itemRepository,
                       ArtRepository artRepository,
                       ElectronicsRepository electronicsRepository,
                       VehicleRepository vehicleRepository,
                       UserRepository userRepository,
                       AuctionRepository auctionRepository,
                       BidHistoryRepository bidHistoryRepository,
                       NotificationService notificationService,
                       Map<String, ItemFactory> itemFactoryRegistry) {
        this.itemRepository = itemRepository;
        this.artRepository = artRepository;
        this.electronicsRepository = electronicsRepository;
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.bidHistoryRepository = bidHistoryRepository;
        this.notificationService = notificationService;
        this.itemFactoryRegistry = itemFactoryRegistry;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public List<ItemResponse> getAllItemResponses() {
        List<ItemResponse> responses = new ArrayList<>();
        for (Item item : getAllItems()) {
            Categories category = item.getCategories();
            if (category == null) {
                continue;
            }
            ItemFactory factory = itemFactoryRegistry.get(category.name());
            if (factory != null) {
                ItemResponse response = factory.mapToResponse(item);
                auctionRepository.findByItem_Id(item.getId()).ifPresent(auction -> {
                    response.setPrice(auction.getCurrentPrice());
                    response.setBidCount((int) bidHistoryRepository.countByAuctionId(auction.getId()));
                });
                responses.add(response);
            }
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
            item.setImageUrls(String.join(",", savedFiles));
            //lưu món hàng vào database
            savedItem = itemRepository.save(item);
            createAuctionForItem(savedItem);
            notificationService.notifyUser(
                    sellerId,
                    "LISTING_CREATED",
                    "Listing created",
                    "Your auction listing " + savedItem.getName() + " has been created.",
                    savedItem.getId(),
                    null
            );
        } catch (Exception e) {
            log.error("Lỗi xử lý lưu sản phẩm tại Server: {}", e.getMessage(), e);
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
            //tìm sản phẩm cũ trong database
            Item existingItem = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            List<String> base64Images = normalizeIncomingImages(itemRequest);
            if (!base64Images.isEmpty()) { //check xem client có gửi ảnh mới không
                deleteExistingImages(existingItem);
                List<String> newFiles = saveImages(base64Images);
                String firstImage = newFiles.isEmpty() ? "no-image.jpg" : newFiles.get(0);
                existingItem.setImageUrl(firstImage);
                existingItem.setImageUrls(String.join(",", newFiles));
                log.info("Đã lưu {} ảnh mới cho item {}", newFiles.size(), id);
            }

            //lấy loại sản phảm
            Categories category = existingItem.getCategories();
            //tìm factory phù hợp
            itemFactory = itemFactoryRegistry.get(category.name());
            //sửa thông tin sản phẩm
            itemFactory.updateItem(existingItem, itemRequest);
            //lưu sản phẩm
            savedItem = itemRepository.save(existingItem);
        }catch (Exception e) {
            log.error("Lỗi khi update sản phẩm {}", e.getMessage());
        }

        //trả về
        return itemFactory.mapToResponse(savedItem);
    }

    public void deleteItem(Long id) {   //xóa sản phẩm
        itemRepository.deleteById(id);
    }

    private void createAuctionForItem(Item item) {
        if (auctionRepository.findByItem_Id(item.getId()).isPresent()) {
            return;
        }
        auctionRepository.save(Auction.fromItem(item));
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
            log.info("Đã lưu ảnh tại {}", imageFile.getAbsolutePath());
        }
        return savedFiles;
    }

    private void ensureUploadDirExists() {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
    }

    private void deleteExistingImages(Item existingItem) {
        if (existingItem.getImageUrls() != null && !existingItem.getImageUrls().isBlank()) {
            for (String imageName : existingItem.getImageUrls().split(",")) {
                deleteImageFile(imageName);
            }
            return;
        }
        deleteImageFile(existingItem.getImageUrl());
    }

    private void deleteImageFile(String imageName) {
        if (imageName == null || imageName.isBlank() || "no-image.jpg".equals(imageName) || "no-image.png".equals(imageName)) {
            return;
        }
        File oldFile = new File(UPLOAD_DIR + File.separator + imageName);
        if (oldFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            oldFile.delete();
            log.info("Đã xóa file ảnh cũ: {}", imageName);
        }
    }
}
