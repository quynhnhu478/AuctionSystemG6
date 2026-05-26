package com.auction.server.repository;

import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.ItemResponse;
import com.auction.server.model.item.Item;
import com.auction.server.model.user.User;

public interface ItemFactory<T extends ItemRequest> {
    Item createItem(T request, String savedFileName, User seller);
    void updateItem(Item item, T request);
    ItemResponse mapToResponse(Item item);
}
