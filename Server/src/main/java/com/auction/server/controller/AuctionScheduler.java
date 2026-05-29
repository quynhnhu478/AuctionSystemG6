package com.auction.server.controller;

import com.auction.common.enums.AuctionStatus;
import com.auction.server.model.Auction;
import com.auction.server.model.Notification;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.NotificationRepository;
import com.auction.server.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;
    private final NotificationRepository notificationRepository;

    @Scheduled(fixedRate = 5000)
    public void scanExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();

        List<Auction> expiredRunning = auctionRepository.findByStatusAndEndTimeBefore(AuctionStatus.RUNNING.toString(), now);
        List<Auction> expiredOpen = auctionRepository.findByStatusAndEndTimeBefore(AuctionStatus.OPEN.toString(), now);

        if (!expiredRunning.isEmpty() || !expiredOpen.isEmpty()) {
            log.info("Phát hiện {} phiên hết giờ ({} RUNNING, {} OPEN). Tiến hành đóng phiên...",
                    expiredRunning.size() + expiredOpen.size(), expiredRunning.size(), expiredOpen.size());

            for (Auction auction : expiredRunning) {
                try {
                    auctionService.endAuction(auction.getId());
                } catch (Exception e) {
                    log.error("Lỗi đóng phiên {}: {}", auction.getId(), e.getMessage());
                }
            }
            for (Auction auction : expiredOpen) {
                try {
                    auctionService.endAuction(auction.getId());
                } catch (Exception e) {
                    log.error("Lỗi đóng phiên {}: {}", auction.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * Sửa dữ liệu bị kẹt: Phiên đấu giá status=FINISHED nhưng người thắng đã thanh toán
     * (notification WINNER_CONFIRM có handled=true) → cập nhật lại thành PAID
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void repairPaidAuctions() {
        List<Auction> finishedAuctions = auctionRepository.findByStatusAndEndTimeBefore(
                AuctionStatus.FINISHED.toString(), LocalDateTime.now());

        for (Auction auction : finishedAuctions) {
            List<Notification> paidNotis = notificationRepository
                    .findByAuctionIdAndTypeAndHandledTrue(auction.getId(), "WINNER_CONFIRM");
            if (!paidNotis.isEmpty()) {
                auction.setStatus(AuctionStatus.PAID.toString());
                auctionRepository.save(auction);
                log.info("Sửa dữ liệu: Phiên {} đã thanh toán, cập nhật FINISHED → PAID", auction.getId());
            }
        }
    }
}