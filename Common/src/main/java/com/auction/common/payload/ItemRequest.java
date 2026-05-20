package com.auction.common.payload;

import com.auction.common.enums.Categories;

public class ItemRequest {
    private String name;
    private String description;
    private Double price;
    private Enum<Categories> categories;
    private Long sellerId;

    public ItemRequest(String name, String description, Double price, Enum<Categories> categories, Long sellerId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categories = categories;
        this.sellerId = sellerId;
    }
    public ItemRequest() {}

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Double getPrice() {
        return price;
    }
    public void setPrice(Double price) {
        this.price = price;
    }
    public Enum<Categories> getCategories() {
        return categories;
    }
    public void setCategories(Enum<Categories> categories) {
        this.categories = categories;
    }
    public Long getSellerId() {
        return sellerId;
    }
    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }
}

