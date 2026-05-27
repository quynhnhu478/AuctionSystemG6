package com.auction.server.repository;

import com.auction.server.model.item.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item,Long> {
    List<Item> findBySeller_Id(Long sellerId);
}
