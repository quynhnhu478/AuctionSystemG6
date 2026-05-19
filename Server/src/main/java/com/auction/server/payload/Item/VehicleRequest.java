package com.auction.server.payload.Item;

import com.auction.server.model.Item.Categories;

public class VehicleRequest extends ItemRequest {
    public VehicleRequest() {
        super();
    }
    public VehicleRequest(String name, String description, Double price, Enum<Categories> categories, Long sellerId) {
        super(name, description, price, categories, sellerId);
    }
}
