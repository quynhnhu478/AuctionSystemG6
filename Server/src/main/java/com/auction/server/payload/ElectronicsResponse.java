package com.auction.server.payload;

import com.auction.server.model.Categories;
import lombok.Getter;
import lombok.Setter;

public class ElectronicsResponse extends ItemResponse{
    @Getter
    @Setter
    private String brand;
    @Getter
    @Setter
    private String warrantyPeriod;

    public ElectronicsResponse() {
        super();
    }
    public ElectronicsResponse(Long id, String name, Double price, String description, Enum<Categories> categories, Long sellerId, String brand, String warrantyPeriod) {
        super(id, name, price, description, categories, sellerId);
        this.brand = brand;
        this.warrantyPeriod = warrantyPeriod;
    }
}
