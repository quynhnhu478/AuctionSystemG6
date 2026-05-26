package com.auction.common.payload;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;
import java.util.List;

public class ItemRequest {
    private String name;
    private String description;
    private Double price;
    private Double bidIncrement;
    private LocalDateTime startingTime;
    private LocalDateTime endTime;
    private Categories categories;
    private String imageBase64;
    private List<String> imageBase64List;
    private Long sellerId;

    public ItemRequest(String name, String description, Double price, Double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, Categories categories, String imageBase64) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.bidIncrement =  bidIncrement;
        this.startingTime = startingTime;
        this.endTime = endTime;
        this.categories = categories;
        this.imageBase64 = imageBase64;
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
    public Categories getCategories() {
        return categories;
    }
    public void setCategories(Categories categories) {
        this.categories = categories;
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

    public String getImageBase64() {
        return imageBase64;
    }
    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public List<String> getImageBase64List() {
        return imageBase64List;
    }

    public void setImageBase64List(List<String> imageBase64List) {
        this.imageBase64List = imageBase64List;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }
}
