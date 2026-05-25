package com.auction.server.service;

import com.auction.common.enums.Categories;
import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.ItemResponse;
import com.auction.server.model.item.Item;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ItemService {
    private static final String UPLOAD_DIR = "uploads/items/";
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    // Bản đồ lưu trữ các Factory đăng ký trong hệ thống
    private final Map<String, ItemFactory> itemFactoryRegistry = new HashMap<>();

    @Autowired
    public ItemService(ItemRepository itemRepository,
                       UserRepository userRepository,
                       List<ItemFactory> factories) { // SỬA TẠI ĐÂY: Nhận một List chứa các Bean Factory có sẵn
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;

        // Tự động chuyển đổi từ List cấu hình sang Map theo cơ chế chuẩn của Spring
        if (factories != null) {
            for (ItemFactory factory : factories) {
                // Thao tác lấy tên Component được định nghĩa tại các class Factory (Ví dụ: "ELECTRONICS", "Art", "VEHICLE")
                org.springframework.stereotype.Component componentAnno = factory.getClass().getAnnotation(org.springframework.stereotype.Component.class);

                // ĐÃ FIX CHUẨN: Kiểm tra cả componentAnno và giá trị value() của nó xem có hợp lệ không bằng dấu TOÁN TỬ NGẮN MẠCH (&&)
                if (componentAnno != null && componentAnno.value() != null && !componentAnno.value().trim().isEmpty()) {
                    String beanName = componentAnno.value().toUpperCase(); // Chuyển chữ hoa hết để chống lệch ký tự
                    this.itemFactoryRegistry.put(beanName, factory);
                } else {
                    // Dự phòng: Nếu dính Proxy DevTools hoặc value() bị rỗng, ta lấy trực tiếp tên Class gốc
                    String className = factory.getClass().getSimpleName();

                    // Nếu tên class dính Proxy dạng ArtFactory$$SpringCGLIB, ta cắt bỏ phần đuôi sau $$ đi
                    if (className.contains("$$")) {
                        className = className.split("\\$\\$")[0];
                    }

                    // Cắt bỏ chữ "Factory" ở đuôi và viết hoa lên (Ví dụ: "ArtFactory" -> "ART")
                    String beanName = className.replace("Factory", "").toUpperCase();

                    this.itemFactoryRegistry.put(beanName, factory);
                }
            }
        }
    }
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Item getItemById(Long id) {
        return itemRepository.findById(id).orElse(null);
    }

    public ItemResponse addItem(ItemRequest itemRequest, Long sellerId) {
        ItemFactory itemFactory = null;
        Item savedItem = null;
        try {
            String savedFileName = null;
            String base64 = itemRequest.getImageBase64();
            if (base64 != null && !base64.isEmpty()) {
                File dir = new File(UPLOAD_DIR);
                if (!dir.exists()) {
                    dir.mkdirs(); // Sử dụng mkdirs() an toàn hơn để tạo chuỗi folder
                }

                savedFileName = UUID.randomUUID().toString() + ".jpg";
                byte[] imageBytes = Base64.getDecoder().decode(base64);
                File imageFile = new File(UPLOAD_DIR + savedFileName);

                try (OutputStream os = new FileOutputStream(imageFile)) {
                    os.write(imageBytes);
                }
                log.info("Photo saved at {} ", imageFile.getAbsolutePath());
            } else {
                savedFileName = "no-image.jpg";
            }

            User seller = userRepository.findById(sellerId).orElseThrow(() -> new RuntimeException("No seller with ID found: " + sellerId));
            Enum<Categories> category = itemRequest.getCategories();

            itemFactory = itemFactoryRegistry.get(category.name().toUpperCase());
            if (itemFactory == null) {
                throw new RuntimeException("No suitable Factory was found for the category: " + category.name());
            }

            Item item = itemFactory.createItem(itemRequest, savedFileName, seller);
            savedItem = itemRepository.save(item);
        } catch (Exception e) {
            log.error("Error processing product save at Server: {}", e.getMessage(), e);
        }
        return itemFactory != null ? itemFactory.mapToResponse(savedItem) : null;
    }

    public ItemResponse updateItem(Long id, ItemRequest itemRequest) {
        ItemFactory itemFactory = null;
        Item savedItem = null;
        try {
            Item existingItem = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("No products found"));

            String base64Image = itemRequest.getImageBase64();
            if (base64Image != null && !base64Image.isEmpty()) {
                String oldFileName = existingItem.getImageUrl();
                if (oldFileName != null && !oldFileName.equals("no-image.jpg") && !oldFileName.equals("no-image.png")) {
                    File oldFile = new File(UPLOAD_DIR + oldFileName);
                    if (oldFile.exists()) {
                        oldFile.delete();
                        log.info("Deleted old image files: {}", oldFileName);
                    }
                }

                String newFileName = UUID.randomUUID().toString() + ".jpg";
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                File newFile = new File(UPLOAD_DIR + newFileName);
                try (OutputStream os = new FileOutputStream(newFile)) {
                    os.write(imageBytes);
                }
                existingItem.setImageUrl(newFileName);
                log.info("New image file saved: {}", newFileName);
            }

            Enum<Categories> category = existingItem.getCategories();

            itemFactory = itemFactoryRegistry.get(category.name().toUpperCase());
            if (itemFactory == null) {
                throw new RuntimeException("No corresponding Factory found: " + category.name());
            }

            itemFactory.updateItem(existingItem, itemRequest);
            savedItem = itemRepository.save(existingItem);

        } catch (Exception e) {
            log.error("Error while updating product: {}", e.getMessage(), e);
        }

        return itemFactory != null ? itemFactory.mapToResponse(savedItem) : null;
    }

    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }
}