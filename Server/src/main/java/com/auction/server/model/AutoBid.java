package com.auction.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

    @Column(name = "increment_amount", nullable = false)
    private double increment;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
