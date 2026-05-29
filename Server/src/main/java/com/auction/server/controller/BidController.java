package com.auction.server.controller;

import com.auction.common.payload.AutoBidRequest;
import com.auction.common.payload.AuctionUpdateResponse;
import com.auction.common.payload.BidHistoryResponse;
import com.auction.common.payload.BidRequest;
import com.auction.server.service.BidService;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")
public class BidController {
    // Khởi tạo Logger theo chuẩn SLF4J cho Spring Boot Controller
    private static final Logger log = LoggerFactory.getLogger(BidController.class);

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
    public ResponseEntity<?> placeBid(@RequestParam Long auctionId,
                                      @RequestParam Long userId,
                                      @RequestParam double bidAmount) {
        try{
            log.info("Nhận yêu cầu đặt giá (Place Bid) - AuctionID: {}, UserID: {}, Số tiền: {}", auctionId, userId, bidAmount);
            AuctionUpdateResponse response = bidService.ProcessPlaceBid(auctionId, userId, bidAmount);
            return ResponseEntity.ok(response);
        } catch (PessimisticLockException | LockTimeoutException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Another bid is being processed. Please try again."));
        } catch (Exception ex){
            log.error("Lỗi xảy ra trong quá trình đặt giá cho phiên đấu giá mã số {}: {}", auctionId, ex.getMessage(), ex);
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/auto-register")
    public ResponseEntity<AuctionUpdateResponse> registerAutoBid(
            @RequestParam Long auctionId,
            @RequestParam Long userId,
            @RequestParam double maxBid) {

        log.info("Nhận yêu cầu kích hoạt đấu giá tự động (Auto Bid) - AuctionID: {}, UserID: {}, Giới hạn tối đa: {}", auctionId, userId, maxBid);

        // Chạy logic cấu hình robot tự động nâng giá
        AuctionUpdateResponse response = bidService.registerAutoBid(auctionId, userId, maxBid);

        // Trả về thông báo cài đặt thành công cho người thực hiện
        return ResponseEntity.ok(response);
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BidHistoryResponse>> getBidsByUser(
            @PathVariable Long userId,
            @RequestParam(value = "category", required = false) String category) {
        return ResponseEntity.ok(bidService.getBidsByUser(userId, category));
    }
}
