package com.auction.server.model.item;

import com.auction.server.model.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

@Entity
@Table(name = "electronics")
public class Electronics extends Item {
    @Column
    @Getter
    @Setter
    private String brand;

    @Column
    @Getter
    @Setter
    private String warrantyPeriod;

    public Electronics() {
        super();
    }
    public Electronics(String name, Categories categories, String description, double price, double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, String imageUrl, User seller, String brand, String warrantyPeriod) {
        super(name, categories, description, price, bidIncrement, startingTime, endTime, imageUrl, seller);
        this.brand = brand;
        this.warrantyPeriod = warrantyPeriod;
    }
}
