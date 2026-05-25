package com.auction.server.service;

import com.auction.server.model.Auction;
import com.auction.server.model.BidHistory;
import com.auction.server.model.user.User;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidHistoryRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate; // Dùng để update realtime sản phẩm

    public AuctionService(AuctionRepository auctionRepository, BidHistoryRepository bidHistoryRepository,
                          UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.auctionRepository = auctionRepository;
        this.bidHistoryRepository = bidHistoryRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public synchronized boolean placeBid(Long userId, Long auctionId, double bidAmount) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("No auction found."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("No user found"));

        // Kiểm tra thời gian kết thúc (so sánh với LocalDateTime của item)
        if (auction.getItem().getEndTime().isBefore(LocalDateTime.now()) || "FINISHED".equals(auction.getStatus())) {
            return false;
        }

        // Giá đặt mới phải lớn hơn giá hiện tại + bước giá tối thiểu (bidIncrement)
        if (bidAmount < (auction.getCurrentPrice() + auction.getItem().getBidIncrement())) {
            return false;
        }

        // Cập nhật thông tin đấu giá
        auction.setCurrentPrice(bidAmount);
        auction.setWinnerId(userId);
        auctionRepository.save(auction);

        // Lưu vào lịch sử đặt giá
        BidHistory bid = new BidHistory();
        bid.setAuctionId(auctionId);
        bid.setUserId(userId);
        bid.setBidAmount(bidAmount);
        bid.setBidTime(LocalDateTime.now());
        bidHistoryRepository.save(bid);

        // REALTIME UPDATE: Phát tín hiệu đến toàn bộ Client đang theo dõi sản phẩm này
        messagingTemplate.convertAndSend("/topic/auction/" + auctionId, auction);

        return true;
    }
}