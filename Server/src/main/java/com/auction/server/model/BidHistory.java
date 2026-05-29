package com.auction.server.model;

import com.auction.server.model.BaseEntity;
import com.auction.server.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "bid_history", indexes = {
        @Index(name = "idx_bid_history_user_time", columnList = "user_id,bid_time"),
        @Index(name = "idx_bid_history_auction", columnList = "auction_id")
})
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class BidHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction; // Thay cho Long auctionId cũ

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "bid_amount", nullable = false)
    private double bidAmount;

    @Column(name = "bid_time",nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime bidTime;
}
