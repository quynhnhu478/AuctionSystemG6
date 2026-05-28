package com.auction.server.repository;

import com.auction.server.model.Auction;
import com.auction.server.model.AutoBid;
import com.auction.server.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AutoBidRepository extends JpaRepository<AutoBid, Long> {

    Optional<AutoBid> findByAuctionAndUserAndActiveTrue(Auction auction, User user);}
