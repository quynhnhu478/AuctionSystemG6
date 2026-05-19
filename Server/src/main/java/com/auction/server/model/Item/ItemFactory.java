package com.auction.server.model.Item;

import com.auction.server.model.User.User;
import com.auction.server.payload.Item.ItemRequest;
import com.auction.server.payload.Item.ItemResponse;

public interface ItemFactory<T extends ItemRequest> {
    Item createItem(T request, User seller);
    void updateItem(Item item, T request);
    ItemResponse mapToResponse(Item item);
}
