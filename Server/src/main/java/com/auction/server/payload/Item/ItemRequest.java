package com.auction.server.payload.Item;

import com.auction.server.model.Item.Categories;
import lombok.Setter;
import lombok.Getter;

public class ItemRequest {
    @Setter
    @Getter
    private String name;
    @Setter
    @Getter
    private String description;
    @Setter
    @Getter
    private Double price;
    @Setter
    @Getter
    private Enum<Categories> categories;
    @Setter
    @Getter
    private Long sellerId;

    public ItemRequest(String name, String description, Double price, Enum<Categories> categories, Long sellerId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categories = categories;
        this.sellerId = sellerId;
    }
    public ItemRequest() {}
}

