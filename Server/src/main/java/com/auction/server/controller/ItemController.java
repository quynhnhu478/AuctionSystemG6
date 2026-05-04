package com.auction.server.controller;

import com.auction.server.model.Item;
import com.auction.server.repository.ItemRepository;
import com.auction.server.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Item getItemById(@RequestParam int id) {
        return this.itemService.getItemById(id);
    }

    @GetMapping  //lấy danh sách tất cả sản phẩm
    public List<Item> getAllItems() {
        return this.itemService.getAllItems();
    }

    @PostMapping //thêm sản phẩm mới
    public void addItem(@RequestBody Item item) {
        this.itemService.addItem(item);
    }

    @PutMapping("/id")

}
