package auction.model.observer;

import auction.model.core.Auction;

//Thông báo đến người "hóng" đấu giá
public interface AuctionObserver //class nào muốn nghe -> implements
{
    void onAuctionUpdated(Auction auction, String event);// truyền chính đối tượng được đấu giá vào
}
/*Observer Pattern
trong 1 cuộc đấu giá -> có nhiều đối tượng quan tâm đến như: người đặt giá, giá sản phẩm, sản phẩm được đấu giá
hệ thống thông báo
Thay vì class Auction phải đi thông báo cho từng đối tượng thì nó chỉ cần giữ 1 danh sách "người đăng ký"(Observers)
-> khi có sự thay đổi thì nó sẽ gọi hàm onAuctionUpdated trên danh sách tất cả các đối tượng đó
 */
