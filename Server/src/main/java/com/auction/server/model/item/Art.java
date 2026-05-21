package com.auction.server.model.item;

import com.auction.server.model.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import com.auction.common.enums.Categories;

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
    public Art(String name, Enum<Categories> categories, String description, double price, User seller, String artist, int yearCreated) {
        super(name, categories, description, price, seller);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }
}
