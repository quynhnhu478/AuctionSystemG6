package com.auction.server.model.item;

import com.auction.server.model.BaseEntity;
import com.auction.server.model.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

@Entity
@Table(name = "item")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Item extends BaseEntity {

    @Getter
    @Setter
    @Column
    private String name;

    @Getter
    @Setter
    @Column(name = "categories")
    private String categoriesRaw;

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

    @Getter
    @Setter
    @Lob
    @Column
    private String imageUrls;

    public Item() {}

    public Item(String name, Categories categories, String description, double price, double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, String imageUrl, User seller) {
        this.name = name;
        setCategories(categories);
        this.description = description;
        this.price = price;
        this.bidIncrement = bidIncrement;
        this.startingTime = startingTime;
        this.endTime = endTime;
        this.imageUrl = imageUrl;
        this.seller = seller;
    }

    public Categories getCategories() {
        return parseCategory(categoriesRaw);
    }

    public void setCategories(Categories categories) {
        this.categoriesRaw = categories == null ? null : categories.name();
    }

    private Categories parseCategory(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue.trim().toUpperCase();
        for (Categories category : Categories.values()) {
            if (normalized.equals(category.name()) || normalized.contains(category.name())) {
                return category;
            }
        }
        return null;
    }

    @Getter
    @Setter
    @ManyToOne
    @JoinColumn(name = "id_user", nullable = false, referencedColumnName = "id")
    @JsonIgnore
    private User seller;

}
