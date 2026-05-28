package com.auction.client.service;

public interface AuctionUpdateListener {
    void onAuctionUpdated(double currentPrice, int bidCount);
}