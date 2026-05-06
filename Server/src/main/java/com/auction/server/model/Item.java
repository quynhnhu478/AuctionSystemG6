package com.auction.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
public class Item extends BaseEntity{

    @Getter
    @Setter
    @Column
    private String name;

    @Getter
    @Setter
    @Column
    private Enum<Categories> categories;

    @Getter
    @Setter
    @Column
    private String description;

    @Getter
    @Setter
    @Column
    private double price;

    public Item() {}

    public Item(String name, Enum categories, String description, double price) {
        this.name = name;
        this.categories = categories;
        this.description = description;
        this.price = price;
    }

    @ManyToOne
    @JoinColumn(name = "id_user", nullable = false, referencedColumnName = "id")
    private User user;

}
