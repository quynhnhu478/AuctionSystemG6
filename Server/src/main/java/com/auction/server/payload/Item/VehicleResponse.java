package com.auction.server.payload.Item;

import com.auction.server.model.Item.Categories;

public class VehicleResponse extends ItemResponse {
    public VehicleResponse() {
        super();
    }
    public VehicleResponse(Long id, String name, Double price, String description, Enum<Categories> categories, Long sellerId) {
        super(id, name, price, description, categories, sellerId);
    }
}
