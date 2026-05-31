module Common {
    // Cho phép các module khác (như Client, Server) nhìn thấy các class trong package này
    exports com.auction.common.enums;
    exports com.auction.common.payload;

    // Yêu cầu thư viện Jackson để xử lý JSON (nếu trong các class này có dùng)
    requires com.fasterxml.jackson.databind;
    requires static lombok;

}