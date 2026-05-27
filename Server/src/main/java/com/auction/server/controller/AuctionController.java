package com.auction.server.controller;

import com.auction.server.service.BidService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auction")
public class AuctionController {

    private final BidService bidService;

    public AuctionController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping("/bid")
    public ResponseEntity<?> placeBid(@RequestParam Long userId, @RequestParam Long auctionId, @RequestParam double amount) {
        try {
            // Note: Nhu's placeBid takes (itemId, userId, amount). 
            // In our client, auctionId parameter actually holds itemId.
            bidService.placeBid(auctionId, userId, amount);
            return ResponseEntity.ok("Set price successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Bidding failure: " + e.getMessage());
        }
    }

    @PostMapping("/autobid")
    public ResponseEntity<?> activateAutoBid(@RequestParam Long userId, @RequestParam Long auctionId,
                                             @RequestParam double maxBid, @RequestParam double bidIncrement) {
        try {
            // Note: Nhu's registerAutoBid takes (itemId, userId, maxBid, increment)
            bidService.registerAutoBid(auctionId, userId, maxBid, bidIncrement);
            return ResponseEntity.ok("Auto-bid configured and activated successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Activation failed: " + e.getMessage());
        }
    }
}