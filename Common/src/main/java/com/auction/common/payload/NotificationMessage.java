package com.auction.common.payload;

import java.time.LocalDateTime;

public class NotificationMessage {
    private String type;
    private String title;
    private String message;
    private Long itemId;
    private Long auctionId;
    private LocalDateTime createdAt;

    public NotificationMessage() {
    }

    public NotificationMessage(String type, String title, String message, Long itemId, Long auctionId) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.itemId = itemId;
        this.auctionId = auctionId;
        this.createdAt = LocalDateTime.now();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
