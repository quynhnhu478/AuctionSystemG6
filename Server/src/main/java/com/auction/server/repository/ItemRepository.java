package com.auction.server.repository;

import com.auction.server.model.item.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item,Long> {
    @Query(
            value = """
                    SELECT i.*
                    FROM item i
                    WHERE EXISTS (SELECT 1 FROM arts a WHERE a.id = i.id)
                       OR EXISTS (SELECT 1 FROM electronics e WHERE e.id = i.id)
                       OR EXISTS (SELECT 1 FROM vehicles v WHERE v.id = i.id)
                    """,
            nativeQuery = true
    )
    List<Item> findAllValidAuctionItems();
}
