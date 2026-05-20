package com.auction.common.payload;

import com.auction.common.enums.Categories;

public class VehicleRequest extends ItemRequest {
    public VehicleRequest() {
        super();
    }
    public VehicleRequest(String name, String description, Double price, Enum<Categories> categories, Long sellerId) {
        super(name, description, price, categories, sellerId);
    }
}
