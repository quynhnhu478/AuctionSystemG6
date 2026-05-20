package com.auction.common.payload;

import com.auction.common.enums.Categories;

public class ArtResponse extends ItemResponse {

    private String artist;
    private int yearCreated;

    public ArtResponse(){
        super();
    }
    public ArtResponse(Long id, String name, Double price, String description, Enum<Categories> categories, Long sellerId, String artist, int yearCreated){
        super(id, name, price, description, categories, sellerId);
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
