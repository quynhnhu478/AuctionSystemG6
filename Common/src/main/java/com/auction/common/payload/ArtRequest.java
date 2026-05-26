package com.auction.common.payload;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

public class ArtRequest extends ItemRequest {
    private String artist;
    private int yearCreated;

    public ArtRequest() {
        super();
    }
    public ArtRequest(String name, String description, Double price, Double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, Categories categories, String imageBase64, String artist, int yearCreated) {
        super(name, description, price, bidIncrement, startingTime, endTime, categories, imageBase64);
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
