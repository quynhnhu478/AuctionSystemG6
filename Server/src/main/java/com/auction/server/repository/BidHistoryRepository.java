package com.auction.server.repository;

import com.auction.server.model.BidHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
public interface BidHistoryRepository extends JpaRepository<BidHistory, Long> {
    // Lấy lịch sử đặt giá của một phiên, xếp giá cao nhất lên đầu
    List<BidHistory> findByAuctionIdOrderByBidAmountDesc(Long auctionId);

    List<BidHistory> findByUserIdOrderByBidTimeDesc(Long userId);

    long countByAuctionId(Long auctionId);

    @Query("""
       select b.auction.id, count(b)
       from BidHistory b
       where b.auction.id in :auctionIds
       group by b.auction.id
       """)
    List<Object[]> countByAuctionIds(@Param("auctionIds") Collection<Long> auctionIds);
    void deleteByAuctionId(Long auctionId);
}
