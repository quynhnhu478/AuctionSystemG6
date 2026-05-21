package com.auction.server.model.item;

import com.auction.server.model.BaseEntity;
import com.auction.server.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.auction.common.enums.Categories;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Item extends BaseEntity {

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

    public Item(String name, Enum<Categories> categories, String description, double price, User seller) {
        this.name = name;
        this.categories = categories;
        this.description = description;
        this.price = price;
        this.seller = seller;
    }

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "id_user", nullable = false, referencedColumnName = "id")
    private User seller;

}
