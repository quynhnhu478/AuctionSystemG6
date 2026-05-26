package com.auction.server.repository;

import com.auction.server.model.AutoBid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AutoBidRepository extends JpaRepository<AutoBid, Long> {
    List<AutoBid> findByAuctionIdAndActiveTrueOrderByCreatedAtAsc(Long auctionId);
    Optional<AutoBid> findByUserIdAndAuctionId(Long userId, Long auctionId);
}
