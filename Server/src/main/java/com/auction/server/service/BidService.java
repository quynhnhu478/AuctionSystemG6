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
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final Map<String, com.auction.server.repository.ItemFactory> itemFactoryRegistry;
    private final Map<Long, Object> itemLocks = new ConcurrentHashMap<>();

    public BidService(BidHistoryRepository bidHistoryRepository,
                      ItemRepository itemRepository,
                      UserRepository userRepository,
                      AuctionRepository auctionRepository,
                      AutoBidRepository autoBidRepository,
                      SimpMessagingTemplate messagingTemplate,
                      Map<String, com.auction.server.repository.ItemFactory> itemFactoryRegistry) {
        this.bidHistoryRepository = bidHistoryRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.auctionRepository = auctionRepository;
        this.autoBidRepository = autoBidRepository;
        this.messagingTemplate = messagingTemplate;
        this.itemFactoryRegistry = itemFactoryRegistry;
    }

    public List<com.auction.common.payload.ItemResponse> getBiddedItemsByUserId(Long userId) {
        List<Long> bidHistoryItemIds = bidHistoryRepository.findAuctionIdsByUserId(userId).stream()
                .map(auctionId -> auctionRepository.findById(auctionId).map(a -> a.getItem().getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();

        List<Long> autoBidItemIds = autoBidRepository.findByUserId(userId).stream()
                .map(AutoBid::getItemId)
                .filter(java.util.Objects::nonNull)
                .toList();

        List<Long> combinedItemIds = new java.util.ArrayList<>();
        for (Long id : bidHistoryItemIds) {
            if (!combinedItemIds.contains(id)) combinedItemIds.add(id);
        }
        for (Long id : autoBidItemIds) {
            if (!combinedItemIds.contains(id)) combinedItemIds.add(id);
        }

        List<com.auction.common.payload.ItemResponse> responses = new java.util.ArrayList<>();
        for (Long itemId : combinedItemIds) {
            itemRepository.findById(itemId).ifPresent(item -> {
                com.auction.common.enums.Categories category = item.getCategories();
                if (category != null) {
                    com.auction.server.repository.ItemFactory factory = itemFactoryRegistry.get(category.name());
                    if (factory != null) {
                        responses.add(factory.mapToResponse(item));
                    }
                }
            });
        }
        return responses;
    }

    public List<BidHistoryResponse> getBidHistoryByItemId(Long itemId) {
        return auctionRepository.findByItem_Id(itemId)
                .map(auction -> getBidHistory(auction.getId()))
                .orElse(Collections.emptyList());
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
        synchronized (lockForItem(itemId)) {
            Auction auction = getAuctionForUpdate(itemId);
            Item item = auction.getItem();
            User user = findUser(userId);
            validateAuctionOpen(item, auction);
            validateBidAmount(item, auction, user, bidAmount);

            saveBid(auction, item, user, bidAmount, false);
            runAutoBidCompetition(auction, item);
            update = buildUpdate(auction, "Bid placed successfully", false);
            bidderBalance = user.getBalance();
        }
        update.setBidderBalance(bidderBalance);
        publishUpdate(update);
        return update;
    }

    @Transactional
    public AuctionUpdateResponse registerAutoBid(Long itemId, Long userId, double maxBid, Double increment) {
        AuctionUpdateResponse update;
        synchronized (lockForItem(itemId)) {
            Auction auction = getAuctionForUpdate(itemId);
            Item item = auction.getItem();
            User user = findUser(userId);
            validateAuctionOpen(item, auction);
            if (item.getSeller() != null && item.getSeller().getId().equals(userId)) {
                throw new IllegalArgumentException("Sellers are not allowed to auto-bid on their own items");
            }

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
            update.setBidderBalance(user.getBalance());
        }
        publishUpdate(update);
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
        if (item.getSeller() != null && item.getSeller().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Sellers are not allowed to bid on their own items");
        }
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

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void saveBid(Auction auction, Item item, User user, double amount, boolean automatic) {
        // Hoàn lại tiền cho người giữ giá cao nhất trước đó nếu có và khác người mới
        if (auction.getWinnerId() != null && !auction.getWinnerId().equals(user.getId())) {
            User prevWinner = userRepository.findById(auction.getWinnerId()).orElse(null);
            if (prevWinner != null) {
                double refundAmount = auction.getCurrentPrice();
                prevWinner.setBalance(prevWinner.getBalance() + refundAmount);
                userRepository.save(prevWinner);
                try {
                    messagingTemplate.convertAndSend("/topic/user-" + prevWinner.getId(), "BALANCE_UPDATE:" + prevWinner.getBalance());
                } catch (Exception e) {
                    System.err.println("Failed to send balance update to user " + prevWinner.getId() + ": " + e.getMessage());
                }
            }
        }

        BidHistory bid = new BidHistory();
        bid.setAuctionId(auction.getId());
        bid.setUserId(user.getId());
        bid.setBidAmount(amount);
        bid.setBidTime(LocalDateTime.now());
        bidHistoryRepository.save(bid);

        user.setBalance(user.getBalance() - amount);
        userRepository.save(user);
        try {
            messagingTemplate.convertAndSend("/topic/user-" + user.getId(), "BALANCE_UPDATE:" + user.getBalance());
        } catch (Exception e) {
            System.err.println("Failed to send balance update to user " + user.getId() + ": " + e.getMessage());
        }
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
        AuctionUpdateResponse broadcastUpdate = new AuctionUpdateResponse();
        broadcastUpdate.setItemId(update.getItemId());
        broadcastUpdate.setAuctionId(update.getAuctionId());
        broadcastUpdate.setWinnerId(update.getWinnerId());
        broadcastUpdate.setCurrentPrice(update.getCurrentPrice());
        broadcastUpdate.setEndTime(update.getEndTime());
        broadcastUpdate.setBidCount(update.getBidCount());
        broadcastUpdate.setMessage(update.getMessage());
        broadcastUpdate.setAutomatic(update.getAutomatic());
        broadcastUpdate.setWinnerName(update.getWinnerName());

        try {
            String json = new ObjectMapper().writeValueAsString(broadcastUpdate);
            messagingTemplate.convertAndSend("/topic/auction-" + broadcastUpdate.getItemId(), json);
            messagingTemplate.convertAndSend("/topic/auctions", json);
        } catch (Exception e) {
            System.err.println("Failed to serialize auction update: " + e.getMessage());
        }
    }

    private BidHistoryResponse toResponse(BidHistory bid) {
        BidHistoryResponse response = new BidHistoryResponse();
        response.setId(bid.getId());
        response.setAuctionId(bid.getAuctionId());
        response.setUserId(bid.getUserId());
        response.setBidAmount(bid.getBidAmount());
        response.setBidTime(bid.getBidTime());
        userRepository.findById(bid.getUserId()).ifPresent(user -> response.setBidderName(user.getName()));
        if (response.getBidderName() == null) {
            response.setBidderName("User #" + bid.getUserId());
        }
        return response;
    }
}
