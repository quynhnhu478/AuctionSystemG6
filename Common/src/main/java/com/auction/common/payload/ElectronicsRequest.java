package com.auction.common.payload;

import com.auction.common.enums.Categories;
public class ElectronicsRequest extends ItemRequest {
    private String brand;

    private String warrantyPeriod;
    public ElectronicsRequest(){
        super();
    }
    public ElectronicsRequest(String name, String description, Double price, Enum<Categories> categories, Long sellerId, String brand, String warrantyPeriod) {
        super(name, description, price, categories, sellerId);
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
