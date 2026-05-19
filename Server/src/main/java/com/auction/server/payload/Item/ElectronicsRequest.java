package com.auction.server.payload.Item;

import com.auction.server.model.Item.Categories;
import lombok.Getter;
import lombok.Setter;

public class ElectronicsRequest extends ItemRequest {
    @Setter
    @Getter
    private String brand;

    @Getter
    @Setter
    private String warrantyPeriod;
    public ElectronicsRequest(){
        super();
    }
    public ElectronicsRequest(String name, String description, Double price, Enum<Categories> categories, Long sellerId, String brand, String warrantyPeriod) {
        super(name, description, price, categories, sellerId);
        this.brand = brand;
        this.warrantyPeriod = warrantyPeriod;
    }
}
