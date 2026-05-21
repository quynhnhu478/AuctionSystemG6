package com.auction.server.model.Item;

import com.auction.server.model.User.User;
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
    public Electronics(String name, Enum<Categories> categories, String description, double price, double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, User seller, String brand, String warrantyPeriod) {
        super(name, categories, description, price, bidIncrement, startingTime, endTime, seller);
        this.brand = brand;
        this.warrantyPeriod = warrantyPeriod;
    }
}
