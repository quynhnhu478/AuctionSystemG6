package auction.enums;

public enum ItemStatus {
    PENDING,    // vừa đăng, chờ admin duyệt
    APPROVED,   // đã duyệt, sẵn sàng đấu giá
    REJECTED,   // bị từ chối
    ACTIVE,     // đang trong phiên đấu giá
    SOLD,       // đã bán thành công
    UNSOLD      // hết giờ, không ai mua
}
