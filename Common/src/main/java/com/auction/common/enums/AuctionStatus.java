package com.auction.common.enums;

public enum AuctionStatus {
    PENDING ,//Chờ diễn ra

    ACTIVE, //Đang diễn ra

    ENDED, //Đã hết giờ / Chờ thanh toán

    PAID, //Đã thanh toán thành công / Hoàn thành

    CANCELED ,//Bị hủy - do Admin hoặc không có ai bid
}
