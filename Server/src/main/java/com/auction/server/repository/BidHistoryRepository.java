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

    @Query("""
       select b
       from BidHistory b
       join fetch b.auction a
       join fetch a.item i
       join fetch b.user u
       left join fetch a.winner w
       where u.id = :userId
         and (:category is null or upper(i.categoriesRaw) like concat('%', upper(:category), '%'))
       order by b.bidTime desc
       """)
    List<BidHistory> findByUserIdWithAuctionItem(
            @Param("userId") Long userId,
            @Param("category") String category
    );

    @Query("""
       select distinct u.id
       from BidHistory b
       join b.user u
       where b.auction.id = :auctionId
       """)
    List<Long> findParticipantUserIdsByAuctionId(@Param("auctionId") Long auctionId);

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
