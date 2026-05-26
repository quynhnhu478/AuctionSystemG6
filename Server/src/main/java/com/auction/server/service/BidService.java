package com.auction.server.service;

import com.auction.common.payload.BidHistoryResponse;
import com.auction.server.model.Auction;
import com.auction.server.model.BidHistory;
import com.auction.server.model.item.Item;
import com.auction.server.model.user.User;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidHistoryRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class BidService {
    private final BidHistoryRepository bidHistoryRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;

    public BidService(BidHistoryRepository bidHistoryRepository,
                      ItemRepository itemRepository,
                      UserRepository userRepository,
                      AuctionRepository auctionRepository) {
        this.bidHistoryRepository = bidHistoryRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
    }

    /** @param itemId id sản phẩm (client gửi trong auctionId) */
    public List<BidHistoryResponse> getBidHistoryByItemId(Long itemId) {
        return auctionRepository.findByItem_Id(itemId)
                .map(auction -> getBidHistory(auction.getId()))
                .orElse(Collections.emptyList());
    }

    public List<BidHistoryResponse> getBidHistory(Long auctionId) {
        List<BidHistory> bids = bidHistoryRepository.findByAuctionIdOrderByBidAmountDesc(auctionId);
        List<BidHistoryResponse> responses = new ArrayList<>();
        for (BidHistory bid : bids) {
            responses.add(toResponse(bid));
        }
        responses.sort(Comparator.comparing(BidHistoryResponse::getBidTime).reversed());
        return responses;
    }

    /** @param itemId id sản phẩm (client gửi trong auctionId) */
    @Transactional
    public BidHistoryResponse placeBid(Long itemId, Long userId, double bidAmount) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Auction item not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDateTime now = LocalDateTime.now();
        if (item.getStartingTime() != null && now.isBefore(item.getStartingTime())) {
            throw new IllegalArgumentException("Auction has not started yet");
        }
        if (item.getEndTime() != null && now.isAfter(item.getEndTime())) {
            throw new IllegalArgumentException("Auction has ended");
        }

        double minBid = item.getPrice() + item.getBidIncrement();
        if (bidAmount < minBid) {
            throw new IllegalArgumentException("Bid must be at least $" + String.format("%.2f", minBid));
        }
        if (user.getBalance() < bidAmount) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        Auction auction = getOrCreateAuction(item);

        BidHistory bid = new BidHistory();
        bid.setAuctionId(auction.getId());
        bid.setUserId(userId);
        bid.setBidAmount(bidAmount);
        bid.setBidTime(now);
        BidHistory saved = bidHistoryRepository.save(bid);

        item.setPrice(bidAmount);
        itemRepository.save(item);

        auction.setCurrentPrice(bidAmount);
        auction.setStatus("ACTIVE");
        auctionRepository.save(auction);

        return toResponse(saved);
    }

    public Auction getOrCreateAuction(Item item) {
        return auctionRepository.findByItem_Id(item.getId())
                .orElseGet(() -> auctionRepository.save(Auction.fromItem(item)));
    }

    private BidHistoryResponse toResponse(BidHistory bid) {
        BidHistoryResponse response = new BidHistoryResponse();
        response.setId(bid.getId());
        response.setAuctionId(bid.getAuctionId());
        response.setUserId(bid.getUserId());
        response.setBidAmount(bid.getBidAmount());
        response.setBidTime(bid.getBidTime());
        userRepository.findById(bid.getUserId()).ifPresent(user -> response.setBidderName(user.getName()));
        if (response.getBidderName() == null) {
            response.setBidderName("User #" + bid.getUserId());
        }
        return response;
    }

}
