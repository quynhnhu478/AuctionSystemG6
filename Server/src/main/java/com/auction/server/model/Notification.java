package com.auction.server.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@Setter
public class Notification extends BaseEntity{
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "auction_id")
    private Long auctionId;

    @Column(name = "type", nullable = false)
    private String type; // WINNER_CONFIRM, SELLER_PAYMENT_SUCCESS, etc.

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "is_handled", nullable = false)
    private boolean handled = false;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
