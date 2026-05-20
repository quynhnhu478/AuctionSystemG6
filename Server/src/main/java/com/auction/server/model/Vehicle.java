package com.auction.server.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import com.auction.common.enums.Categories;

@Entity
@Table(name = "vehicles")
public class Vehicle extends Item {
    public Vehicle() {
        super();
    }
    public Vehicle(String name, Enum<Categories> categories, String description, double price, User seller) {
        super(name, categories, description, price, seller);
    }
}
