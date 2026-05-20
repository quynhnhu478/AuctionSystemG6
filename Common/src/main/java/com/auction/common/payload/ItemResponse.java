package com.auction.common.payload;

import com.auction.common.enums.Categories;

public class ItemResponse {
    private Long id;
    private String name;
    private Double price;
    private String description;
    private Enum<Categories> categories;
    private Long sellerId;

    public ItemResponse() {}
    public ItemResponse(Long id, String name, Double price, String description, Enum<Categories> categories, Long sellerId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.categories = categories;
        this.sellerId = sellerId;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Double getPrice() {
        return price;
    }
    public void setPrice(Double price) {
        this.price = price;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
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
