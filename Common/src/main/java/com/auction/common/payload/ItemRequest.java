package com.auction.common.payload;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

public class ItemRequest {
    private String name;
    private String description;
    private Double price;
    private Double bidIncrement;
    private LocalDateTime startingTime;
    private LocalDateTime endTime;
    private Enum<Categories> categories;
    private Long sellerId;

    public ItemRequest(String name, String description, Double price, Double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, Enum<Categories> categories, Long sellerId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.bidIncrement =  bidIncrement;
        this.startingTime = startingTime;
        this.endTime = endTime;
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
    public Double getBidIncrement() {
        return bidIncrement;
    }
    public void setBidIncrement(Double bidIncrement) {
        this.bidIncrement = bidIncrement;
    }
    public LocalDateTime getStartingTime() {
        return startingTime;
    }
    public void setStartingTime(LocalDateTime startingTime) {
        this.startingTime = startingTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}

