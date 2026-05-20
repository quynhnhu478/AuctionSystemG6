package com.auction.server.repository;

import com.auction.server.model.User.SellerRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerRegistrationRepository extends JpaRepository<SellerRegistration, Long> {
    boolean existsByUser1_IdAndStatus(Long userId, String status);
    List<SellerRegistration> findByStatusOrderByCreatedAtDesc(String status);
    List<SellerRegistration> findAllByOrderByCreatedAtDesc();
}
