package com.auction.server.payload;

import com.auction.server.model.Categories;
import lombok.Getter;
import lombok.Setter;

public class ArtRequest extends ItemRequest{
    @Getter
    @Setter
    private String artist;

    @Getter
    @Setter
    private int yearCreated;

    public ArtRequest() {
        super();
    }
    public ArtRequest(String name, String description, Double price, Enum<Categories> categories, Long sellerId, String artist, int yearCreated) {
        super(name, description, price, categories, sellerId);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }
}
