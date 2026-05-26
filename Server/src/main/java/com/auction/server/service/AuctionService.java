package com.auction.server.service;

import com.auction.server.model.Auction;
import com.auction.server.model.BidHistory;
import com.auction.server.model.AutoBid;
import com.auction.server.model.user.User;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.BidHistoryRepository;
import com.auction.server.repository.UserRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.AutoBidRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final AutoBidRepository autoBidRepository;
    private final SimpMessagingTemplate messagingTemplate; // Dùng để update realtime sản phẩm

    public AuctionService(AuctionRepository auctionRepository, BidHistoryRepository bidHistoryRepository,
                          UserRepository userRepository, ItemRepository itemRepository,
                          AutoBidRepository autoBidRepository, SimpMessagingTemplate messagingTemplate) {
        this.auctionRepository = auctionRepository;
        this.bidHistoryRepository = bidHistoryRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.autoBidRepository = autoBidRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public boolean placeBid(Long userId, Long auctionId, double bidAmount) {
        // Sử dụng PESSIMISTIC_WRITE lock thay vì synchronized
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new RuntimeException("No auction found."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("No user found"));

        // Kiểm tra thời gian kết thúc
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

        // Xử lý Anti-sniping: nếu đặt giá trong 30 giây cuối, tự động kéo dài thêm 60 giây
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = auction.getItem().getEndTime();
        if (now.isBefore(endTime) && java.time.temporal.ChronoUnit.SECONDS.between(now, endTime) <= 30) {
            LocalDateTime newEndTime = endTime.plusSeconds(60);
            auction.getItem().setEndTime(newEndTime);
            itemRepository.save(auction.getItem());
        }

        // REALTIME UPDATE: Phát tín hiệu đến toàn bộ Client đang theo dõi
        messagingTemplate.convertAndSend("/topic/auctions", auction);
        messagingTemplate.convertAndSend("/topic/auction/" + auctionId, auction);

        // Kích hoạt chuỗi đấu giá tự động (Auto-Bidding)
        triggerAutoBids(auction);

        return true;
    }

    public boolean activateAutoBid(Long userId, Long auctionId, double maxBid, double bidIncrement) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new RuntimeException("No auction found."));
        
        if (maxBid <= auction.getCurrentPrice()) {
            return false;
        }

        Optional<AutoBid> existingOpt = autoBidRepository.findByUserIdAndAuctionId(userId, auctionId);
        AutoBid autoBid;
        if (existingOpt.isPresent()) {
            autoBid = existingOpt.get();
        } else {
            autoBid = new AutoBid();
            autoBid.setUserId(userId);
            autoBid.setAuctionId(auctionId);
        }

        autoBid.setMaxBid(maxBid);
        autoBid.setBidIncrement(bidIncrement);
        autoBid.setActive(true);
        autoBid.setCreatedAt(LocalDateTime.now());
        autoBidRepository.save(autoBid);

        // Tự động kiểm tra và phản hồi ngay lập tức nếu đủ điều kiện
        triggerAutoBids(auction);
        return true;
    }

    private void triggerAutoBids(Auction auction) {
        boolean autoBidPlaced = true;
        while (autoBidPlaced) {
            autoBidPlaced = false;

            // Tìm toàn bộ AutoBid đang hoạt động của phiên đấu giá này, ưu tiên theo thời gian đăng ký trước
            List<AutoBid> activeAutoBids = autoBidRepository.findByAuctionIdAndActiveTrueOrderByCreatedAtAsc(auction.getId());

            for (AutoBid ab : activeAutoBids) {
                if (ab.getUserId().equals(auction.getWinnerId())) {
                    continue; // Nếu đang là người thắng thì không tự đặt đè lên chính mình
                }

                double requiredIncrement = Math.max(auction.getItem().getBidIncrement(), ab.getBidIncrement());
                double nextBid = auction.getCurrentPrice() + requiredIncrement;

                if (nextBid <= ab.getMaxBid()) {
                    // Đặt giá tự động
                    auction.setCurrentPrice(nextBid);
                    auction.setWinnerId(ab.getUserId());
                    auctionRepository.save(auction);

                    // Lưu lịch sử
                    BidHistory bid = new BidHistory();
                    bid.setAuctionId(auction.getId());
                    bid.setUserId(ab.getUserId());
                    bid.setBidAmount(nextBid);
                    bid.setBidTime(LocalDateTime.now());
                    bidHistoryRepository.save(bid);

                    // Phát tín hiệu realtime cập nhật bảng giá
                    messagingTemplate.convertAndSend("/topic/auctions", auction);
                    messagingTemplate.convertAndSend("/topic/auction/" + auction.getId(), auction);

                    autoBidPlaced = true;
                    break; // Ngắt vòng lặp trong để truy vấn và kiểm tra lượt tiếp theo với giá mới
                } else {
                    // Vượt quá giới hạn, vô hiệu hóa cấu hình auto-bid này
                    ab.setActive(false);
                    autoBidRepository.save(ab);
                }
            }
        }
    }
}