package com.auction.common.BanMoi;

import java.time.LocalDateTime;
public class BidResponse {
    private Long auctionId;
    private double currentPrice;
    private double nextMinBid;       // Giá tối thiểu cho lượt tiếp theo (= currentPrice + bidIncrement)
    private LocalDateTime endTime;    // Trả về phòng trường hợp bị gia hạn giây cuối (Sniper Bid)

    // Thông tin lượt bid mới nhất để hiện lên top đầu lịch sử
    private Long latestBidderId;
    private String latestBidderName;  // Hiện tên người vừa đặt giá cho uy tín
    private double bidAmount;
    private String bidTimeFormatted;  // Định dạng sẵn chuỗi "14:45:20" cho Front-end đỡ phải parse

    public BidResponse(){}
    public BidResponse(Long auctionId, double currentPrice, double nextMinBid, LocalDateTime endTime){
        this.auctionId = auctionId;
        this.currentPrice = currentPrice;
        this.nextMinBid = nextMinBid;
        this.endTime = endTime;

    }
    public Long getAuctionId() {
        return auctionId;
    }
    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    public double getNextMinBid() {
        return nextMinBid;
    }
    public void setNextMinBid(double nextMinBid) {
        this.nextMinBid = nextMinBid;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public Long getLatestBidderId() {
        return latestBidderId;

    }
    public void setLatestBidderId(Long latestBidderId) {
        this.latestBidderId = latestBidderId;
    }
    public String getLatestBidderName() {
        return latestBidderName;
    }
    public void setLatestBidderName(String latestBidderName) {
        this.latestBidderName = latestBidderName;
    }
    public double getBidAmount() {
        return bidAmount;
    }
    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }
    public String getBidTimeFormatted() {
        return bidTimeFormatted;

    }
    public void setBidTimeFormatted(String bidTimeFormatted) {
        this.bidTimeFormatted = bidTimeFormatted;
    }
}
