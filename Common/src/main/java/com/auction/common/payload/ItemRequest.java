package com.auction.common.payload;

import com.auction.common.enums.Categories;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "categories",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ElectronicsRequest.class, name = "ELECTRONICS"),
        @JsonSubTypes.Type(value = ArtRequest.class, name = "ART"),
        @JsonSubTypes.Type(value = VehicleRequest.class, name = "VEHICLE")
})
public class ItemRequest {
    private String name;
    private String description;
    private Double price;
    private Double bidIncrement;
    private LocalDateTime startingTime;
    private LocalDateTime endTime;
    private Categories categories;
    private String imageBase64;

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
}
