package com.auction.server.repository;

import com.auction.server.model.BidHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BidHistoryRepository extends JpaRepository<BidHistory, Long> {
    // Lấy lịch sử đặt giá của một phiên, xếp giá cao nhất lên đầu
    List<BidHistory> findByAuctionIdOrderByBidAmountDesc(Long auctionId);

    List<BidHistory> findByUserIdOrderByBidTimeDesc(Long userId);

    long countByAuctionId(Long auctionId);
}
