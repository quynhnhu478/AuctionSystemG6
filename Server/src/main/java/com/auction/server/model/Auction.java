package com.auction.server.model;

import com.auction.common.enums.AuctionStatus;
import com.auction.server.model.BaseEntity;
import com.auction.server.model.item.Item;
import com.auction.server.model.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "auctions")
@Getter
@Setter
public class Auction {

    @Id
    @Column
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    // Liên kết 1-1 với sản phẩm đấu giá
    @OneToOne
    @JoinColumn(name = "item_id", referencedColumnName = "id", unique = true)
    private Item item;

    // Thiết lập quan hệ Many-to-One tới người đăng bán (Lazy Loading để tối ưu hiệu năng)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "start_price", nullable = false)
    private double startPrice;

    @Column(name = "current_price", nullable = false)
    private double currentPrice;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Thiết lập quan hệ Many-to-One tới người chiến thắng phiên đấu giá (cho phép null khi chưa kết thúc)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id", nullable = true)
    private User winner;

    // Trạng thái phiên: PENDING (Chờ), ACTIVE (Đang diễn ra), FINISHED (Kết thúc)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "bid_increment", nullable = false)
    private double bidIncrement;

    // Danh sách lịch sử đặt giá, tự động sắp xếp theo số tiền cược giảm dần (Cao nhất lên đầu bảng)
    @OneToMany(mappedBy = "auction", fetch = FetchType.LAZY)
    @OrderBy("bidAmount DESC") // Tự động sắp xếp lượt bid cao nhất lên đầu
    private List<BidHistory> bidHistories;

    // Danh sách các cấu hình tự động đấu giá được cài đặt riêng cho phiên này
    @OneToMany(mappedBy = "auction", fetch = FetchType.LAZY)
    private List<AutoBid> autoBids;

    // Cơ chế Khóa lạc quan (Optimistic Locking) dùng để kiểm soát xung đột dữ liệu khi nhiều người cùng đặt giá (Bid) ở giây cuối cùng
    @Version
    private Long version = 0L; // cơ chế kho dùng để kiểm tra giay cuối có người bid

    /**
     * Hàm nhà máy (Factory Method) hỗ trợ chuyển đổi nhanh dữ liệu từ thực thể Item sang phiên Auction tương ứng
     */
    public static Auction fromItem(Item item) {
        Auction auction = new Auction();
        auction.setId(item.getId());
        auction.setItem(item);
        auction.setTitle(item.getName() != null && !item.getName().isBlank()
                ? item.getName()
                : "Auction #" + item.getId());

        // Kiểm tra tính toàn vẹn dữ liệu: Sản phẩm bắt buộc phải có chủ sở hữu (Seller)
        if (item.getSeller() == null) {
            throw new IllegalStateException("Item has no seller");
        }
        auction.setSeller(item.getSeller());
        auction.setStartPrice(item.getPrice());
        auction.setCurrentPrice(item.getPrice());

        // Gán bước giá mặc định (Ưu tiên lấy cấu hình từ Item, nếu không có sẽ tự động tính bằng 5% giá khởi điểm)
        if (item.getBidIncrement() > 0) {
            auction.setBidIncrement(item.getBidIncrement());
        } else {
            auction.setBidIncrement(item.getPrice() * 0.05);
        }

        auction.setStartTime(item.getStartingTime() != null ? item.getStartingTime() : LocalDateTime.now());
        auction.setEndTime(item.getEndTime() != null ? item.getEndTime() : LocalDateTime.now().plusDays(7));

        // Nên kiểm tra thời gian để set trạng thái chính xác ban đầu thay vì fix cứng ACTIVE
        LocalDateTime now = LocalDateTime.now();
        if (auction.getStartTime().isAfter(now)) {
            auction.setStatus(AuctionStatus.PENDING.toString());
        }
        else if (auction.getEndTime().isBefore(now)) {
            auction.setStatus(AuctionStatus.ENDED.toString());
        }
        else {
            auction.setStatus(AuctionStatus.ACTIVE.toString());
        }

        return auction;
    }
}