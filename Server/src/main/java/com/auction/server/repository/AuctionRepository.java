package com.auction.server.repository;

import com.auction.server.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    // Hàm tìm phiên đấu giá dựa vào id của Item
    Optional<Auction> findByItemId(Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.id = :id")
    Optional<Auction> findByIdForUpdate(@Param("id") Long id);
}