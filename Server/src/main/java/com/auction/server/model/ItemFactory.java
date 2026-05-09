package com.auction.server.model;

import com.auction.server.payload.ArtRequest;
import com.auction.server.payload.ItemRequest;
import com.auction.server.payload.ItemResponse;

public interface ItemFactory<T extends ItemRequest> {
    Item createItem(T request, User seller);
    void updateItem(Item item, T request);
    ItemResponse mapToResponse(Item item);
}
