package com.auction.server.model;

import com.auction.server.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "auto_bids")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class AutoBid extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction; // Thay cho Long auctionId cũ


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "max_bid", nullable = false)
    private double maxBid;



    @Column(name = "registered_at", nullable = false,updatable = false)
    @CreatedDate
    private LocalDateTime registeredAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;


}
