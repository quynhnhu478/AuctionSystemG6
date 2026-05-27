package com.auction.server.controller;


import com.auction.server.model.item.Item;
import com.auction.server.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import com.auction.common.payload.ItemResponse;
import com.auction.common.payload.ItemRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private static final Logger log = LoggerFactory.getLogger(ItemController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/my-listings")
    public ResponseEntity<List<ItemResponse>> getMyListings(@RequestParam("userId") Long userId) {
        List<ItemResponse> myItems = itemService.getItemsBySellerId(userId);
        return ResponseEntity.ok(myItems);
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
        log.info("Add item successfully");
        Map<String, Object> message = Map.of(
                "action", "CREATE",
                "message", "Sản phẩm mới vừa được đăng bán: " + itemRequest.getName()
        );
        ItemResponse savedItemResponse = itemService.addItem(itemRequest, sellerId);
        //Gửi thông báo xuống kênh /topic/products
        messagingTemplate.convertAndSend("/topic/users" + sellerId + "/items", (Object) message);
        //gửi vào kênh chung không phân biệt user
        messagingTemplate.convertAndSend("/topic/live-auctions", savedItemResponse);
        return ResponseEntity.ok(savedItemResponse);
    }

    @PutMapping("/{id}")  //cập nhật sản phẩm
    public ResponseEntity<ItemResponse> updateItem(@PathVariable Long id, @RequestBody ItemRequest itemRequest) {
        log.info("Update item successfully");
        Map<String, Object> message = Map.of(
                "action", "UPDATE",
                "message", "Sản phẩm " + itemRequest.getName() + " vừa được cập nhật thông tin chỉnh sửa."
        );
        messagingTemplate.convertAndSend("/topic/users" + id + "/items", (Object) message);
        return ResponseEntity.ok(itemService.updateItem(id, itemRequest));
    }

    @DeleteMapping("/{id}")  //xóa sản phẩm
    public ResponseEntity<String> deleteItem(@PathVariable Long id) {
        try {
            itemService.deleteItem(id);
            log.info("Deleted item with id: {} ", id);
            Map<String, Object> message = Map.of(
                    "action", "DELETE",
                    "message", "Sản phẩm có id " + id + " vừa bị gỡ khỏi sàn đấu giá"
            );
            messagingTemplate.convertAndSend("/topic/users" + id + "/items", (Object) message);
            return ResponseEntity.ok("Item deleted");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error" + e.getMessage());
        }
    }
}