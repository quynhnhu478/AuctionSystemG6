package com.auction.common.payload;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

public class ArtResponse extends ItemResponse {

    private String artist;
    private int yearCreated;

    public ArtResponse(){
        super();
    }
    public ArtResponse(Long id, String name, Double price, Double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, String description, Categories categories, Long sellerId, String artist, int yearCreated){
        super(id, name, price, bidIncrement, startingTime, endTime, description, categories, sellerId);
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