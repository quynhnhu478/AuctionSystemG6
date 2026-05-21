package com.auction.server.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.auction.common.enums.Categories;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
public class Vehicle extends Item {
    public Vehicle() {
        super();
    }
    public Vehicle(String name, Enum<Categories> categories, String description, double price, double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, User seller) {
        super(name, categories, description, price, bidIncrement, startingTime, endTime, seller);
    }
}
