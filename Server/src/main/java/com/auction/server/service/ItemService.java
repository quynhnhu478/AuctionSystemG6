package com.auction.server.service;


import com.auction.common.enums.Categories;
import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.ItemResponse;
import com.auction.server.model.item.Item;

import com.auction.server.model.Auction;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.ItemFactory;
import com.auction.server.model.user.User;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ItemService {
    private static final String UPLOAD_DIR = "uploads/items/";
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    // - Key (String): Là tên của Categories (ví dụ: "ELECTRONICS", "ART").
    // - Value (ItemFactory): Là instance của Factory tương ứng.
    private final Map<String, ItemFactory> itemFactoryRegistry;
    @Autowired
    public ItemService(ItemRepository itemRepository,
                       UserRepository userRepository,
                       AuctionRepository auctionRepository,
                       Map<String, ItemFactory> itemFactoryRegistry) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.itemFactoryRegistry = itemFactoryRegistry;
    }

    public List<ItemResponse> getAllItems() {   //lấy danh sách sản phẩm
        List<Item> items = itemRepository.findAll();
        return items.stream().map(this::mapItemToResponse).toList();
    }

    private ItemResponse mapItemToResponse(Item item) {
        ItemResponse response = new ItemResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setDescription(item.getDescription());
        response.setPrice(item.getPrice());
        response.setBidIncrement(item.getBidIncrement());
        response.setStartingTime(item.getStartingTime());
        response.setEndTime(item.getEndTime());
        response.setCategories(item.getCategories());
        response.setImageUrl(item.getImageUrl());
        if (item.getSeller() != null) {
            response.setSellerId(item.getSeller().getId());
        }
        if (auctionRepository != null) {
            auctionRepository.findByItemId(item.getId()).ifPresent(auction -> {
                response.setPrice(auction.getCurrentPrice());
            });
        }
        return response;
    }

    public Item getItemById(Long id) {  //lấy sản phẩm bằng ID
        return itemRepository.findById(id).orElse(null);
    }

    public ItemResponse addItem(ItemRequest itemRequest, Long sellerId) {   //thêm sản phẩm
        ItemFactory itemFactory = null;
        Item savedItem = null;
        try {
            String savedFileName = null;
            //lấy chuỗi ảnh ra khỏi request để xử lý lưu file cứng
            String base64 = itemRequest.getImageBase64();
            if (base64 != null && !base64.isEmpty()) {
                //tạo thư mục nếu chưa có
                File dir = new File(UPLOAD_DIR);
                if (!dir.exists()) {
                    dir.mkdirs(); //nếu chưa có thư mục tên uploads/items thì lệnh dir.mkdirs sẽ tạo folder mới
                }

                //sinh tên file ngẫu nhiên bằng UUID chống trùng lặp
                savedFileName = UUID.randomUUID().toString() + ".jpg";

                //Giải mã Base64 thành byte[] và ghi ra ổ cứng Server
                byte[] imageBytes = Base64.getDecoder().decode(base64);
                File imageFile = new File(UPLOAD_DIR + savedFileName);  //khai báo một file mới nằm trong thư mục uploads/items/tenUUIDngaunhien

                try (OutputStream os = new FileOutputStream(imageFile)) {
                    os.write(imageBytes);  //đưa dữ liệu imageBytes vào file imageFile thông qua FileOutputStream
                }
                log.info("Đã lưu ảnh tại {} ", imageFile.getAbsolutePath());
            } else {
                savedFileName = "no-image.jpg";
            }

            //lấy thông tin người bán từ database
            User seller = userRepository.findById(sellerId).orElseThrow(() -> new RuntimeException("Không tìm thấy người bán với ID: " + sellerId));
            //lấy category từ request
            Categories category = itemRequest.getCategories();
            //tìm factory tương ứng
            itemFactory = itemFactoryRegistry.get(category.name());
            if (itemFactory == null) {
                throw new RuntimeException("No suitable Factory was found for the category: " + category.name());
            }
            //khởi tạo món hàng mới thông qua factory
            Item item = itemFactory.createItem(itemRequest, savedFileName, seller);
            //lưu món hàng vào database
            savedItem = itemRepository.save(item);
            
            if (savedItem == null) {
                throw new RuntimeException("Failed to save item to database");
            }

            // Tự động khởi tạo phiên đấu giá cho sản phẩm mới
            Auction auction = new Auction();
            auction.setItem(savedItem);
            auction.setCurrentPrice(savedItem.getPrice());
            auction.setStatus("ACTIVE"); // Thiết lập trạng thái hoạt động trực tiếp
            auctionRepository.save(auction);
            
            //Chuyển đổi Entity thành DTO Response và trả về cho Controller
            return itemFactory.mapToResponse(savedItem);
        } catch (Exception e) {
            log.error("Lỗi xử lý lưu sản phẩm tại Server: {}", e.getMessage());
            throw new RuntimeException("Failed to add item: " + e.getMessage(), e);
        }
    }

    public ItemResponse updateItem(Long id, ItemRequest itemRequest) {   //chỉnh sửa thông tin sản phẩm
        ItemFactory itemFactory = null;
        Item savedItem = null;
        try {
            //tìm sản phẩm cũ trong database
            Item existingItem = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            //lấy base64 từ request
            String base64Image = itemRequest.getImageBase64();
            if (base64Image != null && !base64Image.isEmpty()) { //check xem client có gửi ảnh mới không
                //1.Xóa file ảnh cũ trên ổ cứng (nếu có)
                String oldFileName = existingItem.getImageUrl();
                if (oldFileName != null && !oldFileName.equals("no-image.png")) {
                    File oldFile = new File(UPLOAD_DIR + oldFileName);
                    if (oldFile.exists()) {
                        oldFile.delete(); //xóa file cũ để giải phóng bộ nhớ Server
                        log.info("Đã xóa file ảnh cũ: {}", oldFileName);
                    }
                }
                //2.Ghi file mới
                String newFileName = UUID.randomUUID().toString() + ".jpg";
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                File newFile = new File(UPLOAD_DIR + newFileName);
                try (OutputStream os = new FileOutputStream(newFile)) {
                    os.write(imageBytes);
                }
                //cập nhật tên file mới vào db
                existingItem.setImageUrl(newFileName);
                log.info("Đã lưu file ảnh mới: {}", newFileName);
                //Nếu base64Image == null, ta không làm gì cả (JPA giữ nguyên tên file cũ)

                //lấy loại sản phảm
                Categories category = existingItem.getCategories();
                //tìm factory phù hợp
                itemFactory = itemFactoryRegistry.get(category.name());
                //sửa thông tin sản phẩm
                itemFactory.updateItem(existingItem, itemRequest);
                //lưu sản phẩm
                savedItem = itemRepository.save(existingItem);
            }
        }catch (Exception e) {
            log.error("Lỗi khi update sản phẩm {}", e.getMessage());
        }

        //trả về
        return itemFactory.mapToResponse(savedItem);
    }

    public void deleteItem(Long id) {   //xóa sản phẩm
        itemRepository.deleteById(id);
    }
}