package com.auction.server.controller;

import com.auction.server.model.Notification; // Thay bằng package Notification của bạn
import com.auction.server.repository.NotificationRepository; // Thay bằng package Repository của bạn
import com.auction.server.service.AuctionService; // Thay bằng package AuctionService của bạn
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*") // Cho phép ứng dụng JavaFX kết nối không bị chặn CORS
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuctionService auctionService;

    /**
     * API 1: Lấy danh sách lịch sử thông báo của một User cụ thể để hiển thị lên chuông
     * URL gọi từ Client (GET): http://localhost:8080/api/notifications/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserNotifications(@PathVariable Long userId) {
        try {
            // Lấy danh sách thông báo xếp theo ID giảm dần (mới nhất đẩy lên đầu)
            List<Notification> notifications = notificationRepository.findByUserIdOrderByIdDesc(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi lấy danh sách thông báo: " + e.getMessage()));
        }
    }

    /**
     * API 2: Hứng lệnh bấm nút "Thanh toán (accept=true)" hoặc "Từ chối (accept=false)" từ Người thắng cuộc
     * URL gọi từ Client (POST): http://localhost:8080/api/notifications/winner-confirm?notificationId=...&accept=...
     */
    @PostMapping("/winner-confirm")
    public ResponseEntity<?> handleWinnerConfirm(
            @RequestParam Long notificationId,
            @RequestParam boolean accept) {
        try {
            // Gọi xuống hàm handleWinnerConfirm ở tầng Service xử lý tiền bạc mà bạn đã viết
            auctionService.handleWinnerConfirm(notificationId, accept);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Xử lý phản hồi của người thắng cuộc thành công!"
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Trả về lỗi nghiệp vụ (ví dụ: Thông báo đã xử lý rồi, hoặc ví không đủ tiền) -> HTTP 400
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Trả về lỗi hệ thống đột xuất -> HTTP 500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
}