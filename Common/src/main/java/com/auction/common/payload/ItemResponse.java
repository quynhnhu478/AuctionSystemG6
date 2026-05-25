package com.auction.common.payload;

import com.auction.common.enums.Categories;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemResponse {
    private Long id;
    private String name;
    private Double price;
    private Double bidIncrement;
    private LocalDateTime startingTime;
    private LocalDateTime endTime;
    private String description;
    private Enum<Categories> categories;
    private Long sellerId;
    private String imageUrl;

    public ItemResponse(Long id, String name, Double price, Double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, String description, Enum<Categories> categories, Long sellerId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.bidIncrement = bidIncrement;
        this.startingTime = startingTime;
        this.endTime = endTime;
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
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}