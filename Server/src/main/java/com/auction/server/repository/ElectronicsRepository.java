package com.auction.server.repository;

import com.auction.server.model.item.Electronics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ElectronicsRepository extends JpaRepository<Electronics, Long> {
}
