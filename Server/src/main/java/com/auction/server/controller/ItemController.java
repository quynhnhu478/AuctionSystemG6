package com.auction.server.controller;


import com.auction.server.model.item.Item;
import com.auction.server.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.auction.common.payload.ItemResponse;
import com.auction.common.payload.ItemRequest;

import java.util.List;

@RestController
@RequestMapping("/api/item/")
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
    public List<Item> getAllItems() {
        return this.itemService.getAllItems();
    }

    @PostMapping //thêm sản phẩm mới
    public ResponseEntity<ItemResponse> addItem(@RequestBody ItemRequest itemRequest, @RequestHeader("Seller-ID") Long sellerId) {
        return ResponseEntity.ok(itemService.addItem(itemRequest,  sellerId));
    }

    @PutMapping("/{id}")  //cập nhật sản phẩm
    public ResponseEntity<ItemResponse> updateItem(@PathVariable Long id, @RequestBody ItemRequest itemRequest) {
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
