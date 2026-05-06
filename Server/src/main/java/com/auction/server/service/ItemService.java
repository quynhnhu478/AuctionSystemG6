package com.auction.server.service;

import com.auction.server.model.Item;
import com.auction.server.payload.ItemRequest;
import com.auction.server.payload.ItemResponse;
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

    public Item getItemById(Long id) {  //lấy sản phẩm bằng ID
        return itemRepository.findById(id).orElse(null);
    }

    public ItemResponse addItem(ItemRequest itemRequest) {   //thêm sản phẩm
        // 1. CHUẨN BỊ LƯU DB: Lấy dữ liệu từ Request (do Client gửi) chuyển sang Entity
        Item item = new Item();
        item.setName(itemRequest.getName());
        item.setDescription(itemRequest.getDescription());
        item.setPrice(itemRequest.getPrice());
        item.setCategories(itemRequest.getCategories());
        // 2. LƯU VÀO DATABASE: Lúc này database sẽ tự động cấp một mã ID cho 'savedItem'
        Item savedItem = itemRepository.save(item);
        // 3. ĐÓNG GÓI TRẢ VỀ: Chuyển dữ liệu từ Entity (đã có ID) sang Response
        ItemResponse response = new ItemResponse();
        response.setId(savedItem.getId());
        response.setName(savedItem.getName());
        response.setDescription(savedItem.getDescription());
        response.setPrice(savedItem.getPrice());
        response.setCategories(savedItem.getCategories());
        // 4. Trả về cho Controller gửi về Client
        return response;
    }

    public Item updateItem(Long id, Item itemDetail) {   //chỉnh sửa thông tin sản phẩm
        Item item = getItemById(id);
        item.setName(itemDetail.getName());
        item.setDescription(itemDetail.getDescription());
        item.setCategories(itemDetail.getCategories());
        item.setPrice(itemDetail.getPrice());
        return itemRepository.save(item);
    }

    public void deleteItem(Long id) {   //xóa sản phẩm
        itemRepository.deleteById(id);
    }
}
