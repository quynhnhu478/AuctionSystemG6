package com.auction.server.repository;

import com.auction.server.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item,Integer> {
}
