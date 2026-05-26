package com.auction.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "auto_bids", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "auction_id"})
})
@Getter
@Setter
public class AutoBid extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "max_bid", nullable = false)
    private double maxBid;

    @Column(name = "bid_increment", nullable = false)
    private double bidIncrement;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
