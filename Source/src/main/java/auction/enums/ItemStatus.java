package auction.enums;

public enum ItemStatus {
    OPEN,       //vừa mới đc rao bán
    RUNNING,    //đấu giá đang diễn ra khi có 1 người vừa mới đặt giá mới
    FINISHED,   //hết giờ đấu giá
    CANCELED,   //đã hủy (do admin/seller)
    PAID        //giao dịch hoàn tất

}
