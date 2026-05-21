package com.auction.common.payload;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

public class VehicleResponse extends ItemResponse{
    public VehicleResponse() {
        super();
    }
    public VehicleResponse(Long id, String name, Double price, Double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, String description, Enum<Categories> categories, Long sellerId) {
        super(id, name, price, bidIncrement, startingTime, endTime, description, categories, sellerId);
    }
}
