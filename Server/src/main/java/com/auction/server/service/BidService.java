package com.auction.server.service;

import com.auction.common.payload.AuctionUpdateResponse;
import com.auction.common.payload.BidHistoryResponse;
import com.auction.server.model.Auction;
import com.auction.server.model.AutoBid;
import com.auction.server.model.BidHistory;
import com.auction.server.model.item.Item;
import com.auction.server.model.user.User;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.AutoBidRepository;
import com.auction.server.repository.BidHistoryRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BidService {
    private static final long ANTI_SNIPING_WINDOW_SECONDS = 30;
    private static final long ANTI_SNIPING_EXTENSION_SECONDS = 60;
    private static final int MAX_AUTO_BID_ROUNDS = 100;

    private final BidHistoryRepository bidHistoryRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final AutoBidRepository autoBidRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final Map<Long, Object> itemLocks = new ConcurrentHashMap<>();

    public BidService(BidHistoryRepository bidHistoryRepository,
                      ItemRepository itemRepository,
                      UserRepository userRepository,
                      AuctionRepository auctionRepository,
                      AutoBidRepository autoBidRepository,
                      SimpMessagingTemplate messagingTemplate,
                      NotificationService notificationService) {
        this.bidHistoryRepository = bidHistoryRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.autoBidRepository = autoBidRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    public List<BidHistoryResponse> getBidHistoryByItemId(Long itemId) {
        return auctionRepository.findByItem_Id(itemId)
                .map(auction -> getBidHistory(auction.getId()))
                .orElse(Collections.emptyList());
    }

    public List<BidHistoryResponse> getBidHistoryByUserId(Long userId) {
        List<BidHistoryResponse> responses = new ArrayList<>();
        for (BidHistory bid : bidHistoryRepository.findByUserIdOrderByBidTimeDesc(userId)) {
            responses.add(toResponse(bid));
        }
        return responses;
    }

    public List<BidHistoryResponse> getBidHistory(Long auctionId) {
        List<BidHistory> bids = bidHistoryRepository.findByAuctionIdOrderByBidAmountDesc(auctionId);
        List<BidHistoryResponse> responses = new ArrayList<>();
        for (BidHistory bid : bids) {
            responses.add(toResponse(bid));
        }
        responses.sort(Comparator.comparing(BidHistoryResponse::getBidTime).reversed());
        return responses;
    }

    @Transactional
    public AuctionUpdateResponse placeBid(Long itemId, Long userId, double bidAmount) {
        AuctionUpdateResponse update;
        Double bidderBalance;
        Long sellerId;
        String itemName;
        synchronized (lockForItem(itemId)) {
            Auction auction = getAuctionForUpdate(itemId);
            Item item = auction.getItem();
            User user = findUser(userId);
            validateAuctionOpen(item, auction);
            validateNotSeller(auction, user);
            validateBidAmount(item, auction, user, bidAmount);

            saveBid(auction, item, user, bidAmount, false);
            runAutoBidCompetition(auction, item);
            update = buildUpdate(auction, "Bid placed successfully", false);
            bidderBalance = user.getBalance();
            sellerId = auction.getSellerId();
            itemName = item.getName();
        }
        update.setBidderBalance(bidderBalance);
        publishUpdate(update);
        notifyBidPlaced(update, sellerId, userId, itemName);
        return update;
    }

    @Transactional
    public AuctionUpdateResponse registerAutoBid(Long itemId, Long userId, double maxBid, Double increment) {
        AuctionUpdateResponse update;
        Long sellerId;
        String itemName;
        synchronized (lockForItem(itemId)) {
            Auction auction = getAuctionForUpdate(itemId);
            Item item = auction.getItem();
            User user = findUser(userId);
            validateAuctionOpen(item, auction);
            validateNotSeller(auction, user);

            double effectiveIncrement = normalizeIncrement(item, increment);
            double minBid = auction.getCurrentPrice() + effectiveIncrement;
            if (maxBid < minBid) {
                throw new IllegalArgumentException("Auto-bid max must be at least $" + String.format("%.2f", minBid));
            }
            if (user.getBalance() < maxBid) {
                throw new IllegalArgumentException("Insufficient balance for auto-bid limit");
            }

            AutoBid autoBid = autoBidRepository.findByItemIdAndUserIdAndActiveTrue(itemId, userId)
                    .orElseGet(() -> {
                        AutoBid created = new AutoBid();
                        created.setItemId(itemId);
                        created.setAuctionId(auction.getId());
                        created.setUserId(userId);
                        created.setRegisteredAt(LocalDateTime.now());
                        created.setActive(true);
                        return created;
                    });
            autoBid.setMaxBid(maxBid);
            autoBid.setIncrement(effectiveIncrement);
            autoBidRepository.save(autoBid);

            if (!userId.equals(auction.getWinnerId())) {
                double bidAmount = Math.min(maxBid, auction.getCurrentPrice() + effectiveIncrement);
                if (bidAmount > auction.getCurrentPrice()) {
                    saveBid(auction, item, user, bidAmount, true);
                }
            }
            runAutoBidCompetition(auction, item);
            update = buildUpdate(auction, "Auto-bid activated", true);
            sellerId = auction.getSellerId();
            itemName = item.getName();
        }
        publishUpdate(update);
        notificationService.notifyUser(
                userId,
                "AUTO_BID_ACTIVE",
                "Auto-bid activated",
                "Your auto-bid is active for " + safeItemName(itemName) + ".",
                itemId,
                update.getAuctionId()
        );
        if (sellerId != null && !sellerId.equals(userId)) {
            notificationService.notifyUser(
                    sellerId,
                    "AUTO_BID_ACTIVE",
                    "Auto-bid registered",
                    "A bidder activated auto-bid for " + safeItemName(itemName) + ".",
                    itemId,
                    update.getAuctionId()
            );
        }
        return update;
    }

    public Auction getOrCreateAuction(Item item) {
        return auctionRepository.findByItem_Id(item.getId())
                .orElseGet(() -> auctionRepository.save(Auction.fromItem(item)));
    }

    private Auction getAuctionForUpdate(Long itemId) {
        return auctionRepository.findByItemIdForUpdate(itemId)
                .orElseGet(() -> {
                    Item item = itemRepository.findById(itemId)
                            .orElseThrow(() -> new IllegalArgumentException("Auction item not found"));
                    return auctionRepository.save(Auction.fromItem(item));
                });
    }

    private void validateAuctionOpen(Item item, Auction auction) {
        LocalDateTime now = LocalDateTime.now();
        if (item.getStartingTime() != null && now.isBefore(item.getStartingTime())) {
            throw new IllegalArgumentException("Auction has not started yet");
        }
        if (auction.getEndTime() != null && now.isAfter(auction.getEndTime())) {
            auction.setStatus("FINISHED");
            auctionRepository.save(auction);
            throw new IllegalArgumentException("Auction has ended");
        }
    }

    private void validateBidAmount(Item item, Auction auction, User user, double bidAmount) {
        double minBid = auction.getCurrentPrice() + normalizeIncrement(item, null);
        if (bidAmount < minBid) {
            throw new IllegalArgumentException("Bid must be at least $" + String.format("%.2f", minBid));
        }
        if (user.getBalance() < bidAmount) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        if (user.getId().equals(auction.getWinnerId())) {
            throw new IllegalArgumentException("You are already the highest bidder");
        }
    }

    private void validateNotSeller(Auction auction, User user) {
        if (auction.getSellerId() != null && auction.getSellerId().equals(user.getId())) {
            throw new IllegalArgumentException("You cannot bid on your own listing");
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void saveBid(Auction auction, Item item, User user, double amount, boolean automatic) {
        if (auction.getWinnerId() != null) {
            User prevWinner = findUser(auction.getWinnerId());
            if (prevWinner.getId().equals(user.getId())) {
                user.setBalance(user.getBalance() - (amount - auction.getCurrentPrice()));
            } else {
                prevWinner.setBalance(prevWinner.getBalance() + auction.getCurrentPrice());
                userRepository.save(prevWinner);
                user.setBalance(user.getBalance() - amount);
                notificationService.notifyUser(
                        prevWinner.getId(),
                        "OUTBID",
                        "You were outbid",
                        "Another bidder is now leading on " + safeItemName(item.getName()) + ".",
                        item.getId(),
                        auction.getId()
                );
            }
        } else {
            user.setBalance(user.getBalance() - amount);
        }

        BidHistory bid = new BidHistory();
        bid.setAuctionId(auction.getId());
        bid.setUserId(user.getId());
        bid.setBidAmount(amount);
        bid.setBidTime(LocalDateTime.now());
        bidHistoryRepository.save(bid);

        userRepository.save(user);
        item.setPrice(amount);
        auction.setCurrentPrice(amount);
        auction.setWinnerId(user.getId());
        auction.setStatus("ACTIVE");
        extendAuctionIfNeeded(auction, item);
        itemRepository.save(item);
        auctionRepository.save(auction);
    }

    private void runAutoBidCompetition(Auction auction, Item item) {
        for (int round = 0; round < MAX_AUTO_BID_ROUNDS; round++) {
            double nextMinimum = auction.getCurrentPrice() + normalizeIncrement(item, null);
            AutoBid candidate = autoBidRepository.findByItemIdAndActiveTrue(item.getId()).stream()
                    .filter(autoBid -> !autoBid.getUserId().equals(auction.getWinnerId()))
                    .filter(autoBid -> autoBid.getMaxBid() >= nextMinimum)
                    .min(autoBidComparator())
                    .orElse(null);

            if (candidate == null) {
                return;
            }

            User user = findUser(candidate.getUserId());
            double step = Math.max(normalizeIncrement(item, null), candidate.getIncrement());
            double amount = Math.min(candidate.getMaxBid(), auction.getCurrentPrice() + step);
            if (amount <= auction.getCurrentPrice() || user.getBalance() < amount) {
                candidate.setActive(false);
                autoBidRepository.save(candidate);
                continue;
            }

            // Check if the current winner has an active auto-bid that can match/beat this amount
            if (auction.getWinnerId() != null) {
                AutoBid winnerAutoBid = autoBidRepository.findByItemIdAndUserIdAndActiveTrue(item.getId(), auction.getWinnerId()).orElse(null);
                if (winnerAutoBid != null) {
                    boolean canMatch = false;
                    if (winnerAutoBid.getMaxBid() > amount) {
                        canMatch = true;
                    } else if (winnerAutoBid.getMaxBid() == amount) {
                        if (winnerAutoBid.getRegisteredAt().isBefore(candidate.getRegisteredAt())) {
                            canMatch = true;
                        } else if (winnerAutoBid.getRegisteredAt().isEqual(candidate.getRegisteredAt())) {
                            if (winnerAutoBid.getId() < candidate.getId()) {
                                canMatch = true;
                            }
                        }
                    }

                    if (canMatch) {
                        User winnerUser = findUser(auction.getWinnerId());
                        if (winnerUser.getBalance() + auction.getCurrentPrice() >= amount) {
                            saveBid(auction, item, winnerUser, amount, true);
                            continue;
                        } else {
                            winnerAutoBid.setActive(false);
                            autoBidRepository.save(winnerAutoBid);
                        }
                    }
                }
            }

            saveBid(auction, item, user, amount, true);
        }
    }

    private Comparator<AutoBid> autoBidComparator() {
        return (first, second) -> {
            int maxBidCompare = Double.compare(second.getMaxBid(), first.getMaxBid());
            if (maxBidCompare != 0) {
                return maxBidCompare;
            }
            int registeredCompare = first.getRegisteredAt().compareTo(second.getRegisteredAt());
            if (registeredCompare != 0) {
                return registeredCompare;
            }
            return Long.compare(first.getId(), second.getId());
        };
    }

    private void extendAuctionIfNeeded(Auction auction, Item item) {
        if (auction.getEndTime() == null) {
            return;
        }
        long remainingSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getEndTime());
        if (remainingSeconds >= 0 && remainingSeconds <= ANTI_SNIPING_WINDOW_SECONDS) {
            LocalDateTime extendedEnd = auction.getEndTime().plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);
            auction.setEndTime(extendedEnd);
            item.setEndTime(extendedEnd);
        }
    }

    private double normalizeIncrement(Item item, Double requestedIncrement) {
        if (requestedIncrement != null && requestedIncrement > 0) {
            return requestedIncrement;
        }
        if (item.getBidIncrement() > 0) {
            return item.getBidIncrement();
        }
        return 1.0;
    }

    private Object lockForItem(Long itemId) {
        return itemLocks.computeIfAbsent(itemId, ignored -> new Object());
    }

    private AuctionUpdateResponse buildUpdate(Auction auction, String message, boolean automatic) {
        AuctionUpdateResponse response = new AuctionUpdateResponse();
        response.setItemId(auction.getItem().getId());
        response.setAuctionId(auction.getId());
        response.setWinnerId(auction.getWinnerId());
        response.setCurrentPrice(auction.getCurrentPrice());
        response.setEndTime(auction.getEndTime());
        response.setBidCount(getBidHistory(auction.getId()).size());
        response.setMessage(message);
        response.setAutomatic(automatic);
        if (auction.getWinnerId() != null) {
            userRepository.findById(auction.getWinnerId()).ifPresent(user -> response.setWinnerName(user.getName()));
        }
        return response;
    }

    private void publishUpdate(AuctionUpdateResponse update) {
        messagingTemplate.convertAndSend("/topic/auction-" + update.getItemId(), update);
    }

    private void notifyBidPlaced(AuctionUpdateResponse update, Long sellerId, Long bidderId, String itemName) {
        String safeName = safeItemName(itemName);
        notificationService.notifyUser(
                bidderId,
                "BID_PLACED",
                "Bid placed",
                "Your bid is now the highest bid for " + safeName + ".",
                update.getItemId(),
                update.getAuctionId()
        );
        if (sellerId != null && !sellerId.equals(bidderId)) {
            notificationService.notifyUser(
                    sellerId,
                    "SELLER_NEW_BID",
                    "New bid received",
                    "Your listing " + safeName + " received a new bid of $" + String.format("%.2f", update.getCurrentPrice()) + ".",
                    update.getItemId(),
                    update.getAuctionId()
            );
        }
    }

    private String safeItemName(String itemName) {
        return itemName == null || itemName.isBlank() ? "this auction" : itemName;
    }

    private BidHistoryResponse toResponse(BidHistory bid) {
        BidHistoryResponse response = new BidHistoryResponse();
        response.setId(bid.getId());
        response.setAuctionId(bid.getAuctionId());
        response.setUserId(bid.getUserId());
        response.setBidAmount(bid.getBidAmount());
        response.setBidTime(bid.getBidTime());
        auctionRepository.findById(bid.getAuctionId()).ifPresent(auction -> {
            response.setItemId(auction.getItem() == null ? null : auction.getItem().getId());
            response.setItemName(auction.getTitle());
            response.setCurrentPrice(auction.getCurrentPrice());
            response.setAuctionEndTime(auction.getEndTime());
            response.setWinningBid(auction.getWinnerId() != null && auction.getWinnerId().equals(bid.getUserId()));
        });
        userRepository.findById(bid.getUserId()).ifPresent(user -> response.setBidderName(user.getName()));
        if (response.getBidderName() == null) {
            response.setBidderName("User #" + bid.getUserId());
        }
        return response;
    }
}
