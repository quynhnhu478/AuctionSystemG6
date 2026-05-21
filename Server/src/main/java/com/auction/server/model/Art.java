package com.auction.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

@Entity
@Table(name = "arts")
public class Art extends Item {
    @Column
    @Getter
    @Setter
    private String artist;

    @Column
    @Getter
    @Setter
    private int yearCreated;

    public Art() {
        super();
    }
    public Art(String name, Enum<Categories> categories, String description, double price, double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, User seller, String artist, int yearCreated) {
        super(name, categories, description, price, bidIncrement, startingTime, endTime, seller);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }
}
