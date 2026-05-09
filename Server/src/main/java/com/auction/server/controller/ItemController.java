package com.auction.server.controller;

import com.auction.server.model.Item;
import com.auction.server.payload.ItemRequest;
import com.auction.server.payload.ItemResponse;
import com.auction.server.payload.RegisterRequest;
import com.auction.server.repository.ItemRepository;
import com.auction.server.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/item/")
public class ItemController {
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
    public ItemResponse addItem(@RequestBody ItemRequest itemRequest) {
        return this.itemService.addItem(itemRequest);
    }

    @PutMapping("/{id}")  //cập nhật sản phẩm
    public ResponseEntity<ItemResponse> updateItem(@PathVariable Long id, @RequestBody ItemRequest itemRequest) {
        return ResponseEntity.ok(itemService.updateItem(id, itemRequest));
    }

    @DeleteMapping("/{id}")  //xóa sản phẩm
    public ResponseEntity<String> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.ok("Item deleted");
    }
}
