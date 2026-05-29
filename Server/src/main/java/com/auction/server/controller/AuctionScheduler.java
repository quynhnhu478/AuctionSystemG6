package com.auction.server.controller;

import com.auction.common.enums.AuctionStatus;
import com.auction.server.model.Auction;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.service.AuctionService;
import com.auction.server.service.BidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService ;

    /**
     * Cứ mỗi 5 giây (5000ms), hệ thống sẽ tự quét DB một lần
     * Tìm các phiên đấu giá đang "ACTIVE" nhưng đã quá "endTime"
     */
    @Scheduled(fixedRate = 5000)
    public void scanExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        // Giả sử trạng thái đang chạy của bạn lưu dưới DB là "ACTIVE" hoặc "OPEN"
        String activeStatus = AuctionStatus.RUNNING.toString();

        // Tìm danh sách các phiên hết giờ
        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndTimeBefore(activeStatus, now);

        if (!expiredAuctions.isEmpty()) {
            log.info("Phát hiện {} phiên đấu giá đã hết thời gian đếm ngược. Tiến hành đóng phiên...", expiredAuctions.size());

            for (Auction auction : expiredAuctions) {
                try {
                    // Gọi đến hàm xử lý đã viết ở bước trước
                    auctionService.endAuction(auction.getId());
                } catch (Exception e) {
                    log.error("Lỗi khi cố gắng đóng phiên đấu giá mã số {}: {}", auction.getId(), e.getMessage(), e);
                }
            }
        }
    }
}