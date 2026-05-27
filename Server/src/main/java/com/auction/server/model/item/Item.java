package com.auction.server.model.item;

import com.auction.server.model.BaseEntity;
import com.auction.server.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Item extends BaseEntity {

    @Getter
    @Setter
    @Column
    private String name;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column
    private Categories categories;

    @Getter
    @Setter
    @Column
    private String description;

    @Getter
    @Setter
    @Column
    private double price;

    @Getter
    @Setter
    @Column
    private double bidIncrement;


    @Getter
    @Setter
    @Column
    private LocalDateTime startingTime;

    @Getter
    @Setter
    @Column
    private LocalDateTime endTime;

    @Getter
    @Setter
    @Column
    private String imageUrl;

    public Item() {}

    public Item(String name, Categories categories, String description, double price, double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, String imageUrl, User seller) {
        this.name = name;
        this.categories = categories;
        this.description = description;
        this.price = price;
        this.bidIncrement = bidIncrement;
        this.startingTime = startingTime;
        this.endTime = endTime;
        this.imageUrl = imageUrl;
        this.seller = seller;
    }

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "id_user", nullable = false, referencedColumnName = "id")
    private User seller;

}
