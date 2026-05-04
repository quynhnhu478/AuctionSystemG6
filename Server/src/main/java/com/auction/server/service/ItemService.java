package com.auction.server.service;

import com.auction.server.model.Item;
import com.auction.server.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    public List<Item> getAllItems() {   //lấy danh sách sản phẩm
        return itemRepository.findAll();
    }

    public Item getItemById(int id) {  //lấy sản phẩm bằng ID
        return itemRepository.findById(id).orElse(null);
    }

    public void addItem(Item item) {   //thêm sản phẩm
        itemRepository.save(item);
    }

    public Item updateItem(int id, Item itemDetail) {   //chỉnh sửa thông tin sản phẩm
        Item item = getItemById(id);
        item.setName(itemDetail.getName());
        item.setDescription(itemDetail.getDescription());
        item.setCategories(itemDetail.getCategories());
        item.setPrice(itemDetail.getPrice());
        return itemRepository.save(item);
    }

    public void deleteItem(int id) {   //xóa sản phẩm
        itemRepository.deleteById(id);
    }
}
