package com.auction.server.repository;

import com.auction.server.model.Auction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    Optional<Auction> findByItem_Id(Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.item.id = :itemId")
    Optional<Auction> findByItemIdForUpdate(@Param("itemId") Long itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findWithLockById(@Param("id") Long id);
    List<Auction> findByItem_IdIn(Collection<Long> itemIds);

    List<Auction> findByStatusAndEndTimeBefore(String status, LocalDateTime dateTime);
}
