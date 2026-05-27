package com.auction.server.repository;

import com.auction.server.model.BidHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BidHistoryRepository extends JpaRepository<BidHistory, Long> {
    // Lấy lịch sử đặt giá của một phiên, xếp giá cao nhất lên đầu
    List<BidHistory> findByAuctionIdOrderByBidAmountDesc(Long auctionId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT b.auctionId FROM BidHistory b WHERE b.userId = :userId")
    List<Long> findAuctionIdsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}