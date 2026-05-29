package com.auction.server.controller;

import com.auction.server.service.AuctionService; // Đảm bảo đúng package AuctionService của bạn
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j // Sử dụng Log của Lombok (hoặc khởi tạo Logger thủ công nếu dự án không dùng Lombok)
@RestController
@RequestMapping("/api/auctions")
@CrossOrigin(origins = "*") // Ngăn chặn lỗi CORS khi ứng dụng JavaFX gọi lên Server
public class AuctionController {

    private final AuctionService auctionService;

    // Dependency Injection qua Constructor (chuẩn kiến trúc Spring)
    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * API Chốt phiên đấu giá thủ công khi một Client chạy hết thời gian
     * URL gọi từ Client: POST http://localhost:8080/api/auctions/end-manual?auctionId=...
     */
    @PostMapping("/end-manual")
    public ResponseEntity<?> endAuctionManual(@RequestParam Long auctionId) {
        // Kiểm tra sơ bộ tham số đầu vào
        if (auctionId == null || auctionId <= 0) {
            log.warn("Yêu cầu chốt phiên thất bại do auctionId không hợp lệ: {}", auctionId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Mã số phiên đấu giá (auctionId) không được để trống hoặc nhỏ hơn 0."));
        }

        try {
            log.info("Nhận tín hiệu chốt phiên tự động từ Client - AuctionID: {}", auctionId);

            // Gọi xuống hàm nghiệp vụ dưới tầng Service xử lý DB và phát sóng chuông báo WebSocket
            auctionService.endAuction(auctionId);

            // Trả về JSON thông báo thành công (Mã HTTP 200 OK)
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Phiên đấu giá mã số " + auctionId + " đã được hệ thống kết thúc và chốt trạng thái ENDED!"
            ));

        } catch (IllegalArgumentException ex) {
            // Xử lý các lỗi nghiệp vụ logic (Không tìm thấy ID phiên, sai logic...) -> Trả về 400 Bad Request
            log.error("Lỗi nghiệp vụ khi cố gắng chốt phiên {}: {}", auctionId, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", ex.getMessage()));

        } catch (Exception ex) {
            // Xử lý lỗi hệ thống đột xuất, lỗi driver DB, nghẽn mạng... -> Trả về 500 Internal Server Error
            log.error("Gặp ngoại lệ nghiêm trọng hệ thống khi chốt phiên {}: {}", auctionId, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống phát sinh trong quá trình đóng phiên: " + ex.getMessage()));
        }
    }
}