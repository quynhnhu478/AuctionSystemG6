package com.auction.server.repository;

import com.auction.server.model.User.SellerRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRegistrationRepository extends JpaRepository<SellerRegistration, Long> {
    boolean existsByUser1_IdAndStatus(Long userId, String status);
}
