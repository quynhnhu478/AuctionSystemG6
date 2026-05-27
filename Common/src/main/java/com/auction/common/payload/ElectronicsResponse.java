package com.auction.common.payload;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

public class ElectronicsResponse extends ItemResponse {
    private String brand;
    private String warrantyPeriod;

    public ElectronicsResponse() {
        super();
    }
    public ElectronicsResponse(Long id, String name, Double price, Double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, String description, Categories categories, Long sellerId, String savedFileName, String brand, String warrantyPeriod) {
        super(id, name, price, bidIncrement, startingTime, endTime, description, categories, sellerId, savedFileName);
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
