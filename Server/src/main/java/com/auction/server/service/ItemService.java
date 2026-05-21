package com.auction.server.service;


import com.auction.common.enums.Categories;
import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.ItemResponse;
import com.auction.server.model.item.Item;

import com.auction.server.model.item.ItemFactory;
import com.auction.server.model.user.User;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    // - Key (String): Là tên của Categories (ví dụ: "ELECTRONICS", "ART").
    // - Value (ItemFactory): Là instance của Factory tương ứng.
    private final Map<String, ItemFactory> itemFactoryRegistry;
    @Autowired
    public ItemService(ItemRepository itemRepository,
                       UserRepository userRepository,
                       Map<String, ItemFactory> itemFactoryRegistry) {
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.itemFactoryRegistry = itemFactoryRegistry;
    }

    public List<Item> getAllItems() {   //lấy danh sách sản phẩm
        return itemRepository.findAll();
    }

    public Item getItemById(Long id) {  //lấy sản phẩm bằng ID
        return itemRepository.findById(id).orElse(null);
    }

    public ItemResponse addItem(ItemRequest itemRequest) {   //thêm sản phẩm
        //lấy thông tin người bán từ database
        User seller = userRepository.findById(itemRequest.getSellerId()).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        //lấy category từ request
        Enum<Categories> category = itemRequest.getCategories();
        //tìm factory tương ứng
        ItemFactory itemFactory = itemFactoryRegistry.get(category);
        //khởi tạo món hàng mới thông qua factory
        Item item = itemFactory.createItem(itemRequest, seller);
        //lưu món hàng vào database
        Item savedItem = itemRepository.save(item);
        //Chuyển đổi Entity thành DTO Response và trả về cho Controller
        return itemFactory.mapToResponse(savedItem);
    }

    public ItemResponse updateItem(Long id, ItemRequest itemRequest) {   //chỉnh sửa thông tin sản phẩm
        //tìm sản phẩm cũ trong database
        Item existingItem = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
        //lấy loại sản phảm
        Enum<Categories> category = existingItem.getCategories();
        //tìm factory phù hợp
        ItemFactory itemFactory = itemFactoryRegistry.get(category.name());
        //sửa thông tin sản phẩm
        itemFactory.updateItem(existingItem, itemRequest);
        //lưu sản phẩm
        Item savedItem = itemRepository.save(existingItem);
        //trả về
        return itemFactory.mapToResponse(savedItem);
    }

    public void deleteItem(Long id) {   //xóa sản phẩm
        itemRepository.deleteById(id);
    }
}
