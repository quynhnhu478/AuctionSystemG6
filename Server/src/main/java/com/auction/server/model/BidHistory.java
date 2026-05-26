package com.auction.server.model;

import com.auction.server.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "bid_history")
@Getter
@Setter
public class BidHistory extends BaseEntity {

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bid_amount", nullable = false)
    private double bidAmount;

    @Column(name = "bid_time", nullable = false)
    private LocalDateTime bidTime;
}