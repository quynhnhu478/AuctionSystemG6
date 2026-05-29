package com.auction.server.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
@Service

public class BidService {
    private static final long ANTI_SNIPING_WINDOW_SECONDS = 30;
    private static final long ANTI_SNIPING_EXTENSION_SECONDS = 60;
    private static final int MAX_AUTO_BID_ROUNDS = 100;

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidHistoryRepository bidHistoryRepository;
    private final AutoBidRepository autoBidRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private static final Logger log = LoggerFactory.getLogger(BidService.class);

    public BidService(UserRepository userRepository,
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
    private void validateSellerCannotBidOwnItem(Auction auction, User user) {
        if (auction.getSeller() != null && user.getId().equals(auction.getSeller().getId())) {
            throw new IllegalArgumentException("Seller cannot bid on their own item.");
        }
    }
    // đặt bid thủ công
    @Transactional
    public AuctionUpdateResponse ProcessPlaceBid(Long auctionId, Long userId, double bidAmount) {
        Auction auction = auctionRepository.findWithLockById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("auction not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));

        validateSellerCannotBidOwnItem(auction, user);

        validateAuctionOpen(auction);
        validBidAmount(auction, user, bidAmount);

        refundPreviousHighestBidder(auction);
        processNewBid(auction, user, bidAmount);
        runAutoBidCompetition(auction);
        auctionRepository.save(auction);

        AuctionUpdateResponse update = buildUpdate(auction, user, "Bid successfully!", false);
        publishUpdate(update);
        return update;
    }

    // kiểm tra trạng thái phiên đấu giá
    private void validateAuctionOpen(Auction auction){
        syncAuctionStatusByTime(auction);
        LocalDateTime now = LocalDateTime.now();
        if (auction.getStartTime() != null && now.isBefore(auction.getStartTime())) {
            throw new IllegalArgumentException("auction start time not enough");

        }
        log.info("Validate auction open - auctionId={}, now={}, startTime={}, endTime={}, status={}",
                auction.getId(),
                now,
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getStatus());
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
    private void validBidAmount(Auction auction, User user, double amount) {
        if (auction.getSeller() != null && user.getId().equals(auction.getSeller().getId())) {
            throw new IllegalArgumentException("sellers cannot bid their Items!");
        }

        if (auction.getWinner() != null && user.getId().equals(auction.getWinner().getId())) {
            throw new IllegalArgumentException("you are holding the highest bid");
        }

        double minBid = auction.getCurrentPrice() + auction.getBidIncrement();
        if (amount < minBid) {
            throw new IllegalArgumentException("auction bid amount not enough");
        }

        if (user.getBalance() < amount) {
            throw new IllegalArgumentException("user's account bid amount not enough");
        }
    }
    // hoàn tiền để họ đấu gí tiếp
    private void refundPreviousHighestBidder(Auction auction) {
        // Lấy trực tiếp Winner hiện tại của phiên trước khi bị thay thế
        User currentWinner = auction.getWinner();

        // Nếu chưa có ai bid (winner = null) thì không cần hoàn tiền
        if (currentWinner != null) {
            double currentPrice = auction.getCurrentPrice();

            // Hoàn lại tiền cho người cũ
            currentWinner.setBalance(currentWinner.getBalance() + currentPrice);

            // Ràng buộc để không bao giờ bị âm freeze_balance do lệch số thực
            double newFreeze = Math.max(0, currentWinner.getFreeze_balance() - currentPrice);
            currentWinner.setFreeze_balance(newFreeze);

            userRepository.save(currentWinner);
        }
    }

    private void processNewBid(Auction auction, User user, double amount) {
        // Đóng băng tiền của người mới
        user.setBalance(user.getBalance() - amount);
        user.setFreeze_balance(user.getFreeze_balance() + amount);
        userRepository.save(user);

        // Lưu lịch sử
        BidHistory newBid = new BidHistory();
        newBid.setAuction(auction);
        newBid.setUser(user);
        newBid.setBidAmount(amount);
        newBid.setBidTime(LocalDateTime.now());
        bidHistoryRepository.save(newBid);

        if (auction.getBidHistories() != null) {
            auction.getBidHistories().add(0, newBid);
        }

        // Cập nhật Winner mới và giá mới cho Auction
        auction.setCurrentPrice(amount);
        auction.setWinner(user);

        extendAuctionIfNeeded(auction);
    }

    //logic anti-snipping
    private void extendAuctionIfNeeded(Auction auction) {
        if (auction.getEndTime() == null) return;

        LocalDateTime now = LocalDateTime.now();
        long remainingSeconds = ChronoUnit.SECONDS.between(now, auction.getEndTime());

        // Nếu bid hợp lệ nằm trong khoảng 30 giây cuối
        if (remainingSeconds >= 0 && remainingSeconds <= ANTI_SNIPING_WINDOW_SECONDS) {
            // Đặt lại giờ kết thúc bằng: Thời gian hiện tại + 60 giây gia hạn
            // Cách này giúp thời gian luôn kéo dài thêm đúng 1 phút kể từ khi có lượt bid cuối cùng.
            LocalDateTime extendedEnd = now.plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);

            auction.setEndTime(extendedEnd);
            if (auction.getItem() != null) {
                auction.getItem().setEndTime(extendedEnd);
            }

            // Đảm bảo trạng thái luôn là ACTIVE vì vừa được gia hạn thêm thời gian
            auction.setStatus(AuctionStatus.ACTIVE.toString());
        }
    }

    //logic tự động đấu giá
    private void runAutoBidCompetition(Auction auction) {
        // Nếu hệ thống đang PENDING thực sự (chưa đến giờ), robot sẽ không làm gì cả
        if (!"ACTIVE".equals(auction.getStatus())) {
            return;
        }

        for (int round = 0; round < MAX_AUTO_BID_ROUNDS; round++) {
            try {
                validateAuctionOpen(auction);
            } catch (IllegalArgumentException e) {
                // Nếu validate báo đã đóng/hết giờ -> Dừng cuộc đua Auto-bid ngay lập tức
                break;
            }
            double nextMinimum = auction.getCurrentPrice() + auction.getBidIncrement();
            List<AutoBid> activeAutoBids = auction.getAutoBids();
            if (activeAutoBids == null) return;

            // Tìm người đặt giá tiếp theo thỏa mãn điều kiện
            AutoBid candidate = activeAutoBids.stream()
                    .filter(AutoBid::isActive)
                    .filter(autoBid -> auction.getWinner() == null || !autoBid.getUser().getId().equals(auction.getWinner().getId()))
                    .filter(autoBid -> autoBid.getMaxBid() >= nextMinimum)
                    .min(Comparator.comparing(AutoBid::getMaxBid).reversed() // Ưu tiên người thông minh/chịu chi hơn trước
                            .thenComparing(AutoBid::getRegisteredAt))       // Nếu bằng tiền, ai đến trước thắng
                    .orElse(null);

            if (candidate == null) return; // Hết người đủ điều kiện -> Dừng vòng đấu
            // ... (Logic trừ tiền, đặt giá và loop tiếp tục) ...
            User autoUser = candidate.getUser();
            double amount = Math.min(candidate.getMaxBid(), auction.getCurrentPrice() + auction.getBidIncrement());

            double requiredAdditionalBalance = amount;
            if (auction.getWinner() != null && autoUser.getId().equals(auction.getWinner().getId())) {
                requiredAdditionalBalance = amount - auction.getCurrentPrice();
            }

            if (amount < nextMinimum || autoUser.getBalance() < requiredAdditionalBalance) {
                candidate.setActive(false);
                autoBidRepository.save(candidate);
                continue;
            }

            refundPreviousHighestBidder(auction);
            processNewBid(auction, autoUser, amount);
        }
    }
    private AuctionUpdateResponse buildUpdate(Auction auction,User user, String message, boolean automatic) {
        AuctionUpdateResponse response = new AuctionUpdateResponse();
        response.setItemId(auction.getItem().getId());
        response.setAuctionId(auction.getId());
        response.setCurrentPrice(auction.getCurrentPrice());
        response.setEndTime(auction.getEndTime());
        response.setMessage(message);
        response.setAutomatic(automatic);
        response.setServerTime(LocalDateTime.now());
        if (user != null) {
            response.setBidderBalance(user.getBalance());
            response.setBidderFreezeBalance(user.getFreeze_balance());
        }
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

    //logic thông báo thời gian thực
    private void publishUpdate(AuctionUpdateResponse update) {
        // Kiểm tra xem hiện tại có đang nằm trong một Transaction (Giao dịch DB) hay không
        if (TransactionSynchronizationManager.isActualTransactionActive()) {

            // ĐĂNG KÝ SỰ KIỆN: Chỉ kích hoạt khi DB đã COMMIT thành công hoàn toàn
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // DB lưu xong rồi mới bắn chuông!
                    simpMessagingTemplate.convertAndSend("/topic/auction-" + update.getAuctionId(), "REFRESH_SIGNAL");
                }
            });

        } else {
            // Phòng hờ nếu hàm này gọi ở nơi không có Transaction thì bắn luôn
            simpMessagingTemplate.convertAndSend("/topic/auction-" + update.getAuctionId(), "REFRESH_SIGNAL");
        }

    }

    //logic đăng ký Auto-bid
    @Transactional
    public AuctionUpdateResponse registerAutoBid(Long auctionId, Long userId, double maxBid) {

        Auction auction = auctionRepository.findWithLockById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("No auctions found"));

        User user = userRepository.findWithLockById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        syncAuctionStatusByTime(auction);
        auctionRepository.save(auction);

        validateSellerCannotBidOwnItem(auction, user);
        // UX: Cho phép đăng ký Auto-bid ngay từ khi trạng thái là PENDING (Chờ diễn ra)
        if (!"PENDING".equals(auction.getStatus()) && !"ACTIVE".equals(auction.getStatus())) {
            throw new IllegalArgumentException("Auction is closed, Auto-bid cannot be installed");
        }

        double minRequired = auction.getCurrentPrice() + auction.getBidIncrement();
        if (maxBid < minRequired) {
            throw new IllegalArgumentException("Maximum price must be greater than or equal: " + minRequired);
        }
        if (user.getBalance() < maxBid) {
            throw new IllegalArgumentException("The balance is not sufficient to establish this price ceiling");
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

        AuctionUpdateResponse update = buildUpdate(auction,user, "Kích hoạt Auto-bid thành công", true);
        publishUpdate(update);

        return update;
    }
    private BidHistoryResponse toResponse(BidHistory bid) {
        BidHistoryResponse response = new BidHistoryResponse();
        response.setId(bid.getId());
        response.setAuctionId(bid.getAuction().getId());
        response.setBidAmount(bid.getBidAmount());
        response.setBidTime(bid.getBidTime());

        if (bid.getAuction() != null) {
            response.setAuctionEndTime(bid.getAuction().getEndTime());

            if (bid.getAuction().getWinner() != null && bid.getUser() != null) {
                response.setWinningBid(
                        bid.getAuction().getWinner().getId().equals(bid.getUser().getId())
                );
            } else {
                response.setWinningBid(false);
            }

            if (bid.getAuction().getItem() != null) {
                response.setItemId(bid.getAuction().getItem().getId());
                response.setItemName(bid.getAuction().getItem().getName());
                response.setCurrentPrice(bid.getAuction().getCurrentPrice());
                String imageUrl = bid.getAuction().getItem().getImageUrl();
                if (imageUrl != null && !imageUrl.isBlank()) {
                    response.setImageUrl(imageUrl.startsWith("/")
                            ? imageUrl
                            : "/uploads/items/" + imageUrl);
                }

                if (bid.getAuction().getItem().getCategories() != null) {
                    response.setCategories(bid.getAuction().getItem().getCategories().name());
                }
            }
        }

        if (bid.getUser() != null) {
            response.setUserId(bid.getUser().getId());
            response.setBidderName(bid.getUser().getName());
        } else {
            response.setBidderName("Ẩn danh");
        }

        return response;
    }
    @Transactional(readOnly = true)
    public List<BidHistoryResponse> getBidsByUser(Long userId) {
        return bidHistoryRepository.findByUserIdOrderByBidTimeDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    private void syncAuctionStatusByTime(Auction auction) {
        LocalDateTime now = LocalDateTime.now();

        if (auction.getStartTime() != null && now.isBefore(auction.getStartTime())) {
            auction.setStatus(AuctionStatus.PENDING.toString());
            return;
        }

        if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
            if (auction.getWinner() == null) {
                auction.setStatus(AuctionStatus.CANCELED.toString());
            } else {
                auction.setStatus(AuctionStatus.ENDED.toString());
            }
            return;
        }

        if (auction.getStartTime() != null
                && auction.getEndTime() != null
                && !now.isBefore(auction.getStartTime())
                && !now.isAfter(auction.getEndTime())) {
            auction.setStatus(AuctionStatus.ACTIVE.toString());
        }
    }
}
