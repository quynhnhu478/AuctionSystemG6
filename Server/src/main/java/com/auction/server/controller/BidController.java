package com.auction.server.controller;

import com.auction.common.payload.AutoBidRequest;
import com.auction.common.payload.AuctionUpdateResponse;
import com.auction.common.payload.BidHistoryResponse;
import com.auction.common.payload.BidRequest;
import com.auction.server.service.BidService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")
public class BidController {
    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @GetMapping("/item/{itemId}")
    public List<BidHistoryResponse> getBidHistory(@PathVariable Long itemId) {
        return bidService.getBidHistoryByItemId(itemId);
    }

    @PostMapping
    public ResponseEntity<?> placeBid(@RequestBody BidRequest request) {
        if (request.getAuctionId() == null || request.getUserId() == null || request.getBidAmount() == null) {
            return ResponseEntity.badRequest().body("auctionId, userId and bidAmount are required");
        }
        try {
            AuctionUpdateResponse saved = bidService.placeBid(
                    request.getAuctionId(),
                    request.getUserId(),
                    request.getBidAmount()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to place bid: " + ex.getMessage()));
        }
    }

    @PostMapping("/auto")
    public ResponseEntity<?> registerAutoBid(@RequestBody AutoBidRequest request) {
        if (request.getAuctionId() == null || request.getUserId() == null || request.getMaxBid() == null) {
            return ResponseEntity.badRequest().body("auctionId, userId and maxBid are required");
        }
        try {
            AuctionUpdateResponse saved = bidService.registerAutoBid(
                    request.getAuctionId(),
                    request.getUserId(),
                    request.getMaxBid(),
                    request.getIncrement()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to activate auto-bid: " + ex.getMessage()));
        }
    }
}
