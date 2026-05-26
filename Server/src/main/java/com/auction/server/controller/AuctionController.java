package com.auction.server.controller;

import com.auction.server.service.AuctionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auction")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping("/bid")
    public ResponseEntity<?> placeBid(@RequestParam Long userId, @RequestParam Long auctionId, @RequestParam double amount) {
        boolean success = auctionService.placeBid(userId, auctionId, amount);
        if (success) {
            return ResponseEntity.ok("Set price successfully!");
        }
        return ResponseEntity.badRequest().body("Bidding failure. Please check the amount again!");
    }

    @PostMapping("/autobid")
    public ResponseEntity<?> activateAutoBid(@RequestParam Long userId, @RequestParam Long auctionId,
                                             @RequestParam double maxBid, @RequestParam double bidIncrement) {
        boolean success = auctionService.activateAutoBid(userId, auctionId, maxBid, bidIncrement);
        if (success) {
            return ResponseEntity.ok("Auto-bid configured and activated successfully!");
        }
        return ResponseEntity.badRequest().body("Activation failed. Make sure max bid is greater than the current price.");
    }
}