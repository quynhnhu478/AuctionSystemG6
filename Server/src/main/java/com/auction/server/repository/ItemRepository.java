package com.auction.server.repository;

import com.auction.server.model.item.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item,Long> {
    @Query("""
       select i
       from Item i
       where upper(i.categoriesRaw) like concat('%', upper(:category), '%')
       """)
    List<Item> findByCategoryName(@Param("category") String category);

    List<Item> findBySeller_Id(Long sellerId);

    @Query("""
       select i
       from Item i
       where i.seller.id = :sellerId
         and upper(i.categoriesRaw) like concat('%', upper(:category), '%')
       """)
    List<Item> findBySellerIdAndCategoryName(
            @Param("sellerId") Long sellerId,
            @Param("category") String category
    );

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
