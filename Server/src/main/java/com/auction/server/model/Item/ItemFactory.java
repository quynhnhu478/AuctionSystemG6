package com.auction.server.model;

import com.auction.common.payload.ItemRequest;
import com.auction.common.payload.ItemResponse;

public interface ItemFactory<T extends ItemRequest> {
    Item createItem(T request, User seller);
    void updateItem(Item item, T request);
    ItemResponse mapToResponse(Item item);
}
