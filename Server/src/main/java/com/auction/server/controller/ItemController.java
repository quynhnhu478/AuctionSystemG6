package com.auction.server.controller;


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

import java.util.List;

@RestController
@RequestMapping({"/api/item", "/api/items"})
public class ItemController {
    private static final Logger log = LoggerFactory.getLogger(ItemController.class);
    @Autowired
    private ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping  //tìm sản phẩm theo id
    @RequestMapping("/{id}")
    public Item getItemById(@PathVariable Long id) {
        return this.itemService.getItemById(id);
    }

    @GetMapping  //lấy danh sách tất cả sản phẩm
    public List<ItemResponse> getAllItems() {
        return this.itemService.getAllItemResponses();
    }

    @PostMapping //thêm sản phẩm mới
    public ResponseEntity<?> addItem(
            @RequestBody ItemRequest itemRequest,
            @RequestHeader(value = "Seller-ID", required = false) String sellerIdHeader) {
        Long sellerId = resolveSellerId(sellerIdHeader, itemRequest.getSellerId());
        if (sellerId == null) {
            return ResponseEntity.badRequest().body("Invalid Seller-ID. Please login again.");
        }
        log.info("Add item successfully for seller {}", sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.addItem(itemRequest, sellerId));
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

    @PutMapping("/{id}")  //cập nhật sản phẩm
    public ResponseEntity<ItemResponse> updateItem(@PathVariable Long id, @RequestBody ItemRequest itemRequest) {
        log.info("Update item successfully");
        return ResponseEntity.ok(itemService.updateItem(id, itemRequest));
    }

    @DeleteMapping("/{id}")  //xóa sản phẩm
    public ResponseEntity<String> deleteItem(@PathVariable Long id) {
        try {
            itemService.deleteItem(id);
            log.info("Deleted item with id: {} ", id);
            return ResponseEntity.ok("Item deleted");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error" + e.getMessage());
        }
    }
}