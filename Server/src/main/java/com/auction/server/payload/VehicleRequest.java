package com.auction.server.payload;

import com.auction.server.model.Categories;

public class VehicleRequest extends ItemRequest {
    public VehicleRequest() {
        super();
    }
    public VehicleRequest(String name, String description, Double price, Enum<Categories> categories, Long sellerId) {
        super(name, description, price, categories, sellerId);
    }
}
