package com.auction.common.payload;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

public class ElectronicsRequest extends ItemRequest {
    private String brand;

    private String warrantyPeriod;
    public ElectronicsRequest(){
        super();
    }
    public ElectronicsRequest(String name, String description, Double price, Double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, Categories categories, String imageBase64, String brand, String warrantyPeriod) {
        super(name, description, price, bidIncrement, startingTime, endTime, categories, imageBase64);
        this.brand = brand;
        this.warrantyPeriod = warrantyPeriod;
    }

    public String getBrand() {
        return brand;
    }
    public void setBrand(String brand) {
        this.brand = brand;
    }
    public String getWarrantyPeriod() {
        return warrantyPeriod;
    }
    public void setWarrantyPeriod(String warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }
}
