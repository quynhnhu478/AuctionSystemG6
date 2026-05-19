package com.auction.server.model.Item;

import com.auction.server.model.User.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

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
    public Electronics(String name, Enum<Categories> categories, String description, double price, User seller, String brand, String warrantyPeriod) {
        super(name, categories, description, price, seller);
        this.brand = brand;
        this.warrantyPeriod = warrantyPeriod;
    }
}
