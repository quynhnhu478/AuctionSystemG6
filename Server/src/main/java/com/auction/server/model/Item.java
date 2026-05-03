package com.auction.server.model;

import jakarta.persistence.*;

@Entity
public class Item {

    @Id
    @GeneratedValue
    private long item_id;

    @Column
    private String name;
    @Column
    private Enum categies;
    @Column
    private String description;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "user_id")
    private User user;
}
