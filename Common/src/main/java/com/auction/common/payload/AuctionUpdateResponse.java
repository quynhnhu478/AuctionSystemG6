package com.auction.common.payload;


import java.time.LocalDateTime;

public class AuctionUpdateResponse {
    private Long itemId;
    private Long auctionId;
    private Long winnerId;
    private String winnerName;
    private Double currentPrice;
    private Double bidderBalance;
    private LocalDateTime endTime;
    private Integer bidCount;
    private String message;
    private Boolean automatic;
    private Double bidderFreezeBalance;

    public AuctionUpdateResponse() {
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

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Double getBidderBalance() {
        return bidderBalance;
    }

    public void setBidderBalance(Double bidderBalance) {
        this.bidderBalance = bidderBalance;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getBidCount() {
        return bidCount;
    }

    public void setBidCount(Integer bidCount) {
        this.bidCount = bidCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getAutomatic() {
        return automatic;
    }

    public void setAutomatic(Boolean automatic) {
        this.automatic = automatic;
    }
    public Double getBidderFreezeBalance() {
        return bidderFreezeBalance;
    }
    public void setBidderFreezeBalance(Double freezeBalance) {
        this.bidderFreezeBalance = freezeBalance;
    }

}