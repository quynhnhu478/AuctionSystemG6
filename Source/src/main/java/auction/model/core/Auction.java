package auction.model.core;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import auction.model.base.Entity;
import auction.model.base.Item;
import auction.enums.ItemStatus;
import auction.model.observer.AuctionObserver;

import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {

    // Anti-sniping(gia hạn phiên đấu giá)
    // nếu bid mới đến trong vòng SNIPE_GUARD giây trước khi kết thúc, kéo dài thêm EXTENSION giây
    private static final int SNIPE_GUARD = 30;//gắn thử mặc định 30s
    private static final int EXTENSION   = 60;//gắn thử mặc định 60s

    private Item item;
    private double currentPrice;
    private ItemStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BidTransaction leadingBid; //giá cao nhất

    private final List<BidTransaction> bidHistory = new ArrayList<>();
    //lưu lại lịch sử đặt giá (ai đặt, số tiền bao nhiêu, vào lúc nào) - hiển thị danh sách cho người xem - cuộc đấu giá có thành công hay ko
    private final List<AuctionObserver> observers  = new ArrayList<>();
    //danh sách người nhận thông báo

    private final ReentrantLock bidLock = new ReentrantLock(); //CONCURRENT BIDDING
    /*có thể có 100 người cùng bấm nút "đấu giá" vào 1 giây
    "khóa an toàn" -> khi một người đặt giá, hệ thống sẽ khóa và những người khác sẽ phải đợi
    xử lí xong cho người thứ 1 (check giá, lưu lịch sử, notify) -> hệ thống mới unlock cho người 2 vào
     */

    public Auction() { super(); }

    public Auction(Item item, int durationMinutes) {
        super();
        this.item = item;
        this.currentPrice = item.getStartingPrice();
        this.status = ItemStatus.OPEN;
        this.startTime = LocalDateTime.now();
        this.endTime = startTime.plusMinutes(durationMinutes);
    }

    public void addObserver(AuctionObserver o) { observers.add(o); }
    //thêm 1 người nghe vào danh sách đăng ký: chỉ cần nhấn vào 1 sản phẩm để xem -> addObserver sẽ thêm bạn vào danh sách nhận thông báo
    public void removeObserver(AuctionObserver o) { observers.remove(o); }
    //khi đóng màn hình hoặc chuyển sang xem món đồ khác -> hệ thống sẽ xóa bạn khỏi danh sách

    private void notifyObservers(String event)
    //thông báo đến OBSERVERS khi có thông tin mới
    {
        for (AuctionObserver o : observers) o.onAuctionUpdated(this, event);
        //event -> BID_PLACED/ EXTENDED/ FINISHED
    }

    //BIDDING
    public BidResult placeBid(String bidderId, String bidderName, double amount) {
        bidLock.lock();//khi bắt đầu đấu giá -> hệ thống sẽ khóa cuộc đấu giá lại
        try //xử lí logic - check giá, cập nhật history, tính toán thời gian
        {
            if (status == ItemStatus.FINISHED || status == ItemStatus.CANCELED || status == ItemStatus.PAID)
            {
                return BidResult.failure("Auction is already closed.");
            }
            if (amount <= currentPrice) {
                return BidResult.failure(String.format("Bid must be higher than current price ($%.0f).", currentPrice));
            }

            //ghi nhận lượt đấu giá -> tìm người dẫn đầu
            BidTransaction tx = new BidTransaction(getId(), bidderId, bidderName, amount);
            bidHistory.add(0, tx);
            leadingBid = tx;
            currentPrice = amount;


            // Anti-sniping: nếu có bid mới trong SNIPE_GUARD giây trc khi đấu giá kết thúc
            if (status == ItemStatus.OPEN || status == ItemStatus.RUNNING) //chỉ đấu giá khi trạng thái ở OPEN & RUNNING
            {
                status = ItemStatus.RUNNING;//khi nhận bid thì sẽ chuyển status -> RUNNING
                long secondsLeft = java.time.Duration.between(LocalDateTime.now(), endTime).toSeconds();
                //lấy mốc thời gian hiện tại so với endTime -> tính ra số giây còn lại
                //to.Seconds() -> chuyển sang đơn vị giây
                if (secondsLeft > 0 && secondsLeft <= SNIPE_GUARD)
                {
                    endTime = endTime.plusSeconds(EXTENSION);//thời gian kết thúc bị đẩy lên thêm EXTENSION giây
                    notifyObservers("EXTENDED");
                }
            }

            notifyObservers("BID_PLACED");
            return BidResult.success(tx);
        }
        finally {
            bidLock.unlock();//mở để cho người khác vào đặt giá
        }
    }

    public void finish() {
        bidLock.lock();
        try {
            if (status == ItemStatus.RUNNING || status == ItemStatus.OPEN) {
                status = bidHistory.isEmpty() ? ItemStatus.CANCELED : ItemStatus.FINISHED;
                notifyObservers("FINISHED");
            }
        } finally {
            bidLock.unlock();
        }
    }

    public void markPaid() //Xác nhận của ADMIN
    {
        if (status == ItemStatus.FINISHED) {
            status = ItemStatus.PAID;
            notifyObservers("PAID");
        }
    }

    public long getSecondsRemaining()
    //luôn dừng ở 00:00 ko chạy đến số âm
    {
        long s = java.time.Duration.between(LocalDateTime.now(), endTime).toSeconds();
        return Math.max(0, s);

    }

    public String getFormattedTimeLeft()
    //chuyển số giây thành định dạng chuẩn giờ, phút, giây
    {
        long total = getSecondsRemaining(); //tổng số giây còn lại
        long h = total / 3600;
        long m = (total % 3600) / 60;
        long s = total % 60;
        return String.format("%dh %02dm %02ds", h, m, s);// %02d: nếu số < 10 -> tự thêm "0" ở trước
    }

    public String getFormattedTimeLeftShort()
    //định dạng short time: chỉ hiện phút và giây
    {
        long total = getSecondsRemaining();
        long m = total / 60;
        long s = total % 60;
        return m + "m " + s + "s";
    }

    @Override
    public String getDisplayInfo() {
        return String.format("Auction[%s] %s — $%.0f (%s)",
                getId().substring(0, 6), item.getName(), currentPrice, status);
    }

    //getter
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public double getCurrentPrice() { return currentPrice; }
    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus s) { this.status = s; }

    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    public BidTransaction getLeadingBid() { return leadingBid; }

    public List<BidTransaction> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }
}
