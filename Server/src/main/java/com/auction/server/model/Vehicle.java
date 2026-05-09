package com.auction.server.model;

public class Vehicle extends Item {
    public Vehicle() {
        super();
    }
    public Vehicle(String name, Enum<Categories> categories, String description, double price, User seller) {
        super(name, categories, description, price, seller);
    }
}
