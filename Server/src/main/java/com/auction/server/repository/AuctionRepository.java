package com.auction.server.repository;

import com.auction.server.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    // Hàm tìm phiên đấu giá dựa vào id của Item
    Optional<Auction> findByItem_Id(Long itemId);
}