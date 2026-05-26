package com.auction.common.payload;
import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

public class VehicleRequest extends ItemRequest {
    public VehicleRequest() {
        super();
    }
    public VehicleRequest(String name, String description, Double price, Double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, Categories categories, String imageBase64) {
        super(name, description, price, bidIncrement, startingTime, endTime, categories, imageBase64);
    }
}
