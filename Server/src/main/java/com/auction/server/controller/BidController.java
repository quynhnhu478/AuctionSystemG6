package com.auction.server.controller;

import com.auction.common.payload.AutoBidRequest;
import com.auction.common.payload.AuctionUpdateResponse;
import com.auction.common.payload.BidHistoryResponse;
import com.auction.common.payload.BidRequest;
import com.auction.server.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")

public class BidController {
    private final BidService bidService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    public BidController(BidService bidService, SimpMessagingTemplate simpMessagingTemplate){
        this.bidService = bidService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }




    @GetMapping("/history/{auctionId}")
    public ResponseEntity<List<BidHistoryResponse>> getBidHistory(@PathVariable Long auctionId) {
        List<BidHistoryResponse> bidHistoryResponse = bidService.getBidHistory(auctionId);
        return ResponseEntity.ok(bidHistoryResponse);
    }

    @PostMapping("/place")
    public ResponseEntity<AuctionUpdateResponse> placeBid(@RequestParam Long auctionId,
                                                          @RequestParam Long userId,
                                                          @RequestParam double bidAmount) {
        AuctionUpdateResponse response = bidService.ProcessPlaceBid(auctionId, userId, bidAmount);

        // Trả về kết quả 200 OK kèm theo số dư ví mới riêng cho người vừa bấm đặt giá
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auto-register")
    public ResponseEntity<AuctionUpdateResponse> registerAutoBid(
            @RequestParam Long auctionId,
            @RequestParam Long userId,
            @RequestParam double maxBid) {

        // Chạy logic cấu hình robot tự động nâng giá
        AuctionUpdateResponse response = bidService.registerAutoBid(auctionId, userId, maxBid);

        // Trả về thông báo cài đặt thành công cho người thực hiện
        return ResponseEntity.ok(response);
    }
}
