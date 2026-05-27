package com.auction.server.service;

import com.auction.common.BanMoi.BidResult;
import com.auction.common.enums.AuctionStatus;
import com.auction.common.payload.AuctionUpdateResponse;
import com.auction.common.payload.BidHistoryResponse;
import com.auction.server.model.Auction;
import com.auction.server.model.AutoBid;
import com.auction.server.model.BidHistory;
import com.auction.server.model.user.User;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.AutoBidRepository;
import com.auction.server.repository.BidHistoryRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BiddService {
    private static final long ANTI_SNIPING_WINDOW_SECONDS = 30;
    private static final long ANTI_SNIPING_EXTENSION_SECONDS = 60;
    private static final int MAX_AUTO_BID_ROUNDS = 100;

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final AutoBidRepository autoBidRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public BiddService(UserRepository userRepository,
                       AuctionRepository auctionRepository,
                       BidHistoryRepository bidHistoryRepository,
                        AutoBidRepository autoBidRepository,
                       SimpMessagingTemplate simpMessagingTemplate) {
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.bidHistoryRepository = bidHistoryRepository;
        this.autoBidRepository = autoBidRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;

    }
    // lấy lịch sử đấu giá
    @Transactional(readOnly = true)
    public List<BidHistoryResponse> getBidHistory(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá"));

        List<BidHistory> bids = auction.getBidHistories(); // Tận dụng @OneToMany có sẵn
        if (bids == null) return Collections.emptyList();

        return bids.stream().map(this::toResponse).collect(Collectors.toList());
    }
    // đặt bid thủ công
    @Transactional
    public AuctionUpdateResponse ProcessPlaceBid(Long auctionId, Long userId, double bidAmount){
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("auction not found"));

        // validateAuctionStatus(auction, userId, bidAmount);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        if (user.getBalance() < bidAmount) {
            throw new IllegalArgumentException("user bid amount not enough");
        }
        validateAuctionOpen(auction);
        validBidAmount(auction, user, bidAmount);

        refundPreviousHighestBidder(auction);
        processNewBid(auction, user, bidAmount);
        runAutoBidCompetition(auction);
        auctionRepository.save(auction);
        AuctionUpdateResponse update = buildUpdate(auction, "Bid successfully!", false);
        update.setBidderBalance(user.getBalance());
        publishUpdate(update);
        return update;


    }

    // kiểm tra trạng thái phiên đấu giá
    private void validateAuctionOpen(Auction auction){
        LocalDateTime now = LocalDateTime.now();
        if (auction.getStartTime() != null && now.isBefore(auction.getStartTime())) {
            throw new IllegalArgumentException("auction start time not enough");

        }
        if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
            if (auction.getWinner() == null){
                auction.setStatus(AuctionStatus.CANCELED.toString());
            }
            else{
                auction.setStatus(AuctionStatus.ENDED.toString());
            }
            auctionRepository.save(auction);
            throw new IllegalArgumentException("Auction has been closed");
        }
        if (AuctionStatus.PENDING.toString().equals(auction.getStatus())){
            auction.setStatus(AuctionStatus.ACTIVE.toString());
            auctionRepository.save(auction);
        }
        if (!auction.getStatus().equals(AuctionStatus.ACTIVE.toString())){
            throw new IllegalArgumentException("auction has been not active");
        }
    }
    // kiểm tra bid
    private void validBidAmount(Auction auction, User user, double amount){
        double minBid = auction.getCurrentPrice() + auction.getBidIncrement();
        if (amount < minBid) {
            throw new IllegalArgumentException("auction bid amount not enough");

        }
        if (user.getBalance() < amount) {
            throw new IllegalArgumentException("user's account bid amount not enough");

        }
        if (auction.getWinner() != null && user.getId().equals(auction.getWinner())){
            throw new IllegalArgumentException("you are holding the highest bid");
        }

    }
    // hoàn tiền để họ đấu gí tiếp
    private void refundPreviousHighestBidder(Auction auction) {
        List<BidHistory> histories = auction.getBidHistories();
        // Do đầu danh sách luôn là lượt đặt giá cao nhất cũ (nhờ @OrderBy tại Entity)
        if (histories != null && !histories.isEmpty()) {
            BidHistory highestOldBid = histories.get(0);
            User oldWinner = highestOldBid.getUser();

            // Hoàn lại tiền khả dụng, giảm tiền đóng băng của người cũ
            oldWinner.setBalance(oldWinner.getBalance() + highestOldBid.getBidAmount());
            oldWinner.setFreeze_balance(oldWinner.getFreeze_balance() - highestOldBid.getBidAmount());
            userRepository.save(oldWinner);
        }
    }

    private void processNewBid(Auction auction, User user, double amount) {
        // Đóng băng tài sản của người ra giá mới
        user.setBalance(user.getBalance() - amount);
        user.setFreeze_balance(user.getFreeze_balance() + amount);
        userRepository.save(user);

        // Lưu log lịch sử
        BidHistory newBid = new BidHistory();
        newBid.setAuction(auction);
        newBid.setUser(user);
        newBid.setBidAmount(amount);
        newBid.setBidTime(LocalDateTime.now());
        bidHistoryRepository.save(newBid);

        // Đồng bộ lên object cha Auction (Không cần update thủ công sang bảng Item nữa)
        auction.setCurrentPrice(amount);
        auction.setWinner(user);

        extendAuctionIfNeeded(auction);
    }
    private void extendAuctionIfNeeded(Auction auction) {
        if (auction.getEndTime() == null) return;

        long remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
        if (remainingSeconds >= 0 && remainingSeconds <= ANTI_SNIPING_WINDOW_SECONDS) {
            LocalDateTime extendedEnd = auction.getEndTime().plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);
            auction.setEndTime(extendedEnd);
            if (auction.getItem() != null) {
                auction.getItem().setEndTime(extendedEnd);
            }
        }

    }
    private void runAutoBidCompetition(Auction auction) {
        // Nếu hệ thống đang PENDING thực sự (chưa đến giờ), robot sẽ không làm gì cả
        if (!"ACTIVE".equals(auction.getStatus())) {
            return;
        }

        for (int round = 0; round < MAX_AUTO_BID_ROUNDS; round++) {
            double nextMinimum = auction.getCurrentPrice() + auction.getBidIncrement();
            List<AutoBid> activeAutoBids = auction.getAutoBids();
            if (activeAutoBids == null) return;

            // Tìm người đặt giá tiếp theo thỏa mãn điều kiện
            AutoBid candidate = activeAutoBids.stream()
                    .filter(AutoBid::isActive)
                    .filter(autoBid -> auction.getWinner() == null || !autoBid.getUser().getId().equals(auction.getWinner().getId()))
                    .filter(autoBid -> autoBid.getMaxBid() >= nextMinimum)
                    .min(Comparator.comparing(AutoBid::getRegisteredAt)) // Xếp hàng FIFO
                    .orElse(null);

            if (candidate == null) return; // Hết người đủ điều kiện -> Dừng vòng đấu

            // ... (Logic trừ tiền, đặt giá và loop tiếp tục) ...
            User autoUser = candidate.getUser();
            double amount = Math.min(candidate.getMaxBid(), auction.getCurrentPrice() + auction.getBidIncrement());

            if (amount <= auction.getCurrentPrice() || autoUser.getBalance() < amount) {
                candidate.setActive(false);
                autoBidRepository.save(candidate);
                continue;
            }

            refundPreviousHighestBidder(auction);
            processNewBid(auction, autoUser, amount);
        }
    }
    private AuctionUpdateResponse buildUpdate(Auction auction, String message, boolean automatic) {
        AuctionUpdateResponse response = new AuctionUpdateResponse();
        response.setItemId(auction.getItem().getId());
        response.setAuctionId(auction.getId());
        response.setCurrentPrice(auction.getCurrentPrice());
        response.setEndTime(auction.getEndTime());
        response.setMessage(message);
        response.setAutomatic(automatic);

        // Không query thêm SQL, bốc thẳng dữ liệu đã nạp trong Entity
        if (auction.getWinner() != null) {
            response.setWinnerId(auction.getWinner().getId());
            response.setWinnerName(auction.getWinner().getName());
        }
        if (auction.getBidHistories() != null) {
            response.setBidCount(auction.getBidHistories().size());
        }
        return response;
    }
    private void publishUpdate(AuctionUpdateResponse update) {
        simpMessagingTemplate.convertAndSend("/topic/auction-" + update.getAuctionId(), update);
    }
    @Transactional
    public AuctionUpdateResponse registerAutoBid(Long auctionId, Long userId, double maxBid) {
        Auction auction = auctionRepository.findWithLockById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        // UX: Cho phép đăng ký Auto-bid ngay từ khi trạng thái là PENDING (Chờ diễn ra)
        if (!"PENDING".equals(auction.getStatus()) && !"ACTIVE".equals(auction.getStatus())) {
            throw new IllegalArgumentException("Phiên đấu giá đã đóng, không thể cài Auto-bid");
        }

        double minRequired = auction.getCurrentPrice() + auction.getBidIncrement();
        if (maxBid < minRequired) {
            throw new IllegalArgumentException("Giá tối đa phải lớn hơn hoặc bằng: " + minRequired);
        }
        if (user.getBalance() < maxBid) {
            throw new IllegalArgumentException("Số dư không đủ để thiết lập mức giá trần này");
        }

        // Tận dụng quan hệ lưu hoặc cập nhật cấu hình AutoBid
        AutoBid autoBid = autoBidRepository.findByAuctionAndUserAndActiveTrue(auction, user)
                .orElseGet(() -> {
                    AutoBid created = new AutoBid();
                    created.setAuction(auction);
                    created.setUser(user);
                    created.setRegisteredAt(LocalDateTime.now());
                    created.setActive(true);
                    return created;
                });
        autoBid.setMaxBid(maxBid);
        autoBidRepository.save(autoBid);

        // Nếu phòng đang ACTIVE và mình chưa giữ Top 1 -> Tự động kích nổ lượt bid đầu tiên
        if ("ACTIVE".equals(auction.getStatus()) && (auction.getWinner() == null || !userId.equals(auction.getWinner().getId()))) {
            double firstBidAmount = Math.min(maxBid, auction.getCurrentPrice() + auction.getBidIncrement());
            if (firstBidAmount >= minRequired) {
                refundPreviousHighestBidder(auction);
                processNewBid(auction, user, firstBidAmount);
            }
        }

        runAutoBidCompetition(auction);
        auctionRepository.save(auction);

        AuctionUpdateResponse update = buildUpdate(auction, "Kích hoạt Auto-bid thành công", true);
        update.setBidderBalance(user.getBalance());
        publishUpdate(update);

        return update;
    }
    private BidHistoryResponse toResponse(BidHistory bid) {
        BidHistoryResponse response = new BidHistoryResponse();
        response.setId(bid.getId());
        response.setAuctionId(bid.getAuction().getId());
        response.setBidAmount(bid.getBidAmount());
        response.setBidTime(bid.getBidTime());

        if (bid.getUser() != null) {
            response.setUserId(bid.getUser().getId());
            response.setBidderName(bid.getUser().getName());
        } else {
            response.setBidderName("Ẩn danh");
        }
        return response;
    }
}
