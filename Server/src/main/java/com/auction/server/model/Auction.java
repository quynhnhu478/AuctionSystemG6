package com.auction.server.model;

import com.auction.server.model.BaseEntity;
import com.auction.server.model.item.Item;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auctions")
@Getter
@Setter
public class Auction extends BaseEntity {

    // Liên kết 1-1 với sản phẩm đấu giá
    @OneToOne
    @JoinColumn(name = "item_id", referencedColumnName = "id", unique = true)
    private Item item;

    @Column(name = "current_price", nullable = false)
    private double currentPrice;

    // ID của người đang trả giá cao nhất (Tạm thời để Long, hoặc bạn có thể map sang @ManyToOne với User nếu muốn)
    @Column(name = "winner_id")
    private Long winnerId;

    // Trạng thái phiên: PENDING (Chờ), ACTIVE (Đang diễn ra), FINISHED (Kết thúc)
    @Column(name = "status", length = 20)
    private String status;
}