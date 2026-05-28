package com.auction.common.payload;

import java.time.LocalDateTime;

public class BidHistoryResponse {
    private Long id;
    private Long auctionId;
    private Long itemId;
    private Long userId;
    private String bidderName;
    private String itemName;
    private Double currentPrice;
    private LocalDateTime auctionEndTime;
    private Boolean winningBid;
    private Double bidAmount;
    private LocalDateTime bidTime;
    private String categories;


    public BidHistoryResponse() {}
    public BidHistoryResponse(Long auctionId, Long userId, String bidderName, Double bidAmount, LocalDateTime bidTime) {
        this.auctionId = auctionId;
        this.userId = userId;
        this.bidderName = bidderName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;

    }
    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDateTime getAuctionEndTime() {
        return auctionEndTime;
    }

    public void setAuctionEndTime(LocalDateTime auctionEndTime) {
        this.auctionEndTime = auctionEndTime;
    }

    public Boolean getWinningBid() {
        return winningBid;
    }

    public void setWinningBid(Boolean winningBid) {
        this.winningBid = winningBid;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(Double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
}
