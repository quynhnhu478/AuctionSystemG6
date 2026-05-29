package com.auction.common.enums;

public enum AuctionStatus {
    OPEN ,//Chờ diễn ra

    RUNNING, //Đang diễn ra

    FINISHED, //Đã hết giờ / Chờ thanh toán

    PAID, //Đã thanh toán thành công / Hoàn thành

    CANCELED ,//Bị hủy - do Admin hoặc không có ai bid
}
