package com.auction.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auto_bids")
@Getter
@Setter
public class AutoBid extends BaseEntity {
    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "max_bid", nullable = false)
    private double maxBid;

    @Column(name = "bid_increment", nullable = false)
    private double increment;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (registeredAt == null) {
            registeredAt = LocalDateTime.now();
        }
    }
}
