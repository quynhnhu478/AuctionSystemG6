package com.auction.common.payload;

import com.auction.common.enums.Categories;

public class ArtRequest extends ItemRequest {
    private String artist;
    private int yearCreated;

    public ArtRequest() {
        super();
    }
    public ArtRequest(String name, String description, Double price, Enum<Categories> categories, Long sellerId, String artist, int yearCreated) {
        super(name, description, price, categories, sellerId);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    public String getArtist() {
        return artist;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }
    public int getYearCreated() {
        return yearCreated;
    }
    public void setYearCreated(int yearCreated) {
        this.yearCreated = yearCreated;
    }
}
