package com.auction.server.payload;

import com.auction.server.model.Categories;
import lombok.Getter;
import lombok.Setter;

public class ArtResponse extends ItemResponse{
    @Getter
    @Setter
    private String artist;

    @Getter
    @Setter
    private int yearCreated;

    public ArtResponse(){
        super();
    }
    public ArtResponse(Long id, String name, Double price, String description, Enum<Categories> categories, Long sellerId, String artist, int yearCreated){
        super(id, name, price, description, categories, sellerId);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }
}
