package com.auction.server.model.item;

import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.ItemResponse;
import com.auction.server.model.user.User;

public interface ItemFactory {
    Item createItem(ItemRequest request, String savedFileName, User seller);
    void updateItem(Item item, ItemRequest request);
    ItemResponse mapToResponse(Item item);
}
