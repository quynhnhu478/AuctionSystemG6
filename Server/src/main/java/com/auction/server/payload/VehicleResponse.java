package com.auction.server.payload;

import com.auction.server.model.Categories;

public class VehicleResponse extends ItemResponse{
    public VehicleResponse() {
        super();
    }
    public VehicleResponse(Long id, String name, Double price, String description, Enum<Categories> categories, Long sellerId) {
        super(id, name, price, description, categories, sellerId);
    }
}
