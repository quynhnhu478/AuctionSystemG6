package com.auction.server.controller;

import com.auction.common.enums.Categories;
import com.auction.server.model.item.Item;
import com.auction.server.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.auction.common.payload.ItemResponse;
import com.auction.common.payload.ItemRequest;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private static final Logger log = LoggerFactory.getLogger(ItemController.class);

    @Autowired
    private ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/{id}")
    public ItemResponse getItemById(@PathVariable Long id) {
        return this.itemService.ItemDetail(id);
    }

    @GetMapping  // Lấy danh sách tất cả sản phẩm
    public ResponseEntity<?> getAllItems(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "sellerId", required = false) Long sellerId) {
        Categories categoryFilter = parseCategory(category);
        if (category != null && !category.isBlank() && categoryFilter == null) {
            return ResponseEntity.badRequest().body("Invalid category: " + category);
        }
        return ResponseEntity.ok(this.itemService.getItemResponses(categoryFilter, sellerId));
    }

    private Categories parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        try {
            return Categories.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @PostMapping // Thêm sản phẩm mới
    public ResponseEntity<?> addItem(
            @RequestBody ItemRequest itemRequest,
            @RequestHeader(value = "Seller-ID", required = false) String sellerIdHeader) {

        Long sellerId = resolveSellerId(sellerIdHeader, itemRequest.getSellerId());
        if (sellerId == null) {
            return ResponseEntity.badRequest().body("Invalid Seller-ID. Please login again.");
        }

        try {
            // Lấy chuỗi mã hóa Base64 gửi từ JavaFX Client qua trường imageBase64
            String rawBase64 = itemRequest.getImageBase64();

            // Log kiểm tra xem Client đã gửi chuỗi lên thành công chưa
            if (rawBase64 != null) {
                log.info("Nhận được ảnh Base64 từ Client với độ dài chuỗi: {}", rawBase64.length());
            } else {
                log.warn("Sản phẩm được thêm không kèm theo chuỗi ảnh Base64!");
            }

            // Gọi tầng Service xử lý nghiệp vụ lưu Database (Spring JPA)
            Object result = itemService.addItem(itemRequest, sellerId);

            log.info("Add item successfully for seller {}", sellerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (Exception e) {
            log.error("Lỗi khi thêm sản phẩm: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    private Long resolveSellerId(String sellerIdHeader, Long sellerIdFromBody) {
        if (sellerIdFromBody != null && sellerIdFromBody > 0) {
            return sellerIdFromBody;
        }
        if (sellerIdHeader == null || sellerIdHeader.isBlank() || "null".equalsIgnoreCase(sellerIdHeader.trim())) {
            return null;
        }
        try {
            return Long.parseLong(sellerIdHeader.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @PutMapping("/{id}")  // Cập nhật sản phẩm
    public ResponseEntity<?> updateItem(@PathVariable Long id, @RequestBody ItemRequest itemRequest) {
        try {
            ItemResponse response = itemService.updateItem(id, itemRequest);
            if (response == null) {
                log.warn("Không thể chỉnh sửa sản phẩm ID: {} (Có thể do phiên đấu giá đã kết thúc)", id);
                return ResponseEntity.badRequest().body("Cannot edit item after auction has ended.");
            }
            log.info("Update item successfully for ID: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Gặp ngoại lệ khi cố gắng cập nhật sản phẩm mã số ID: {}. Chi tiết: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")  // Xóa sản phẩm
    public ResponseEntity<String> deleteItem(@PathVariable Long id) {
        try {
            itemService.deleteItem(id);
            log.info("Deleted item with id: {} ", id);
            return ResponseEntity.ok("Item deleted successfully");
        } catch (Exception e) {
            // Đưa toàn bộ đối tượng Exception 'e' vào log.error để Spring Boot ghi nhận đầy đủ Stack Trace
            log.error("Gặp ngoại lệ khi cố gắng xóa sản phẩm có mã số ID: {}. Chi tiết lỗi: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
