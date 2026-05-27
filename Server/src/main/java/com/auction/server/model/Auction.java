package com.auction.server.model;

import com.auction.server.model.BaseEntity;
import com.auction.server.model.item.Item;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auctions")
@Getter
@Setter
public class Auction extends BaseEntity {

    @Column(name = "title", nullable = false)
    private String title;

    // Liên kết 1-1 với sản phẩm đấu giá
    @OneToOne
    @JoinColumn(name = "item_id", referencedColumnName = "id", unique = true)
    private Item item;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "start_price", nullable = false)
    private double startPrice;

    @Column(name = "current_price", nullable = false)
    private double currentPrice;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "bid_increment", nullable = false)
    private double bidIncrement;

    @Column(name = "winner_id")
    private Long winnerId;

    // Trạng thái phiên: PENDING (Chờ), ACTIVE (Đang diễn ra), FINISHED (Kết thúc)
    @Column(name = "status", length = 20)
    private String status;

    public static Auction fromItem(Item item) {
        Auction auction = new Auction();
        auction.setItem(item);
        auction.setTitle(item.getName() != null && !item.getName().isBlank()
                ? item.getName()
                : "Auction #" + item.getId());
        if (item.getSeller() == null) {
            throw new IllegalStateException("Item has no seller");
        }
        auction.setSellerId(item.getSeller().getId());
        auction.setStartPrice(item.getPrice());
        auction.setCurrentPrice(item.getPrice());
        auction.setBidIncrement(item.getBidIncrement());
        auction.setStartTime(item.getStartingTime() != null ? item.getStartingTime() : LocalDateTime.now());
        auction.setEndTime(item.getEndTime() != null ? item.getEndTime() : LocalDateTime.now().plusDays(7));
        auction.setStatus("ACTIVE");
        return auction;
    }
}