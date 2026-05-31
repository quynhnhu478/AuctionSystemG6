package com.auction.server.service;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.payload.AuctionUpdateResponse;
import com.auction.server.model.Auction;
import com.auction.server.model.BidHistory;
import com.auction.server.model.item.Item;
import com.auction.server.model.item.Vehicle;
import com.auction.server.model.user.User;
import com.auction.server.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BidServiceTest{

    @Autowired
    private BidService bidService;

    @Autowired
    private BidHistoryRepository bidHistoryRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private AutoBidRepository autoBidRepository;

    @MockitoBean  //sử dung @MockitoBean thay thế Bean thật trong Spring Context
    private SimpMessagingTemplate simpMessagingTemplate;

    private User seller;
    private User bidderA;
    private User bidderB;
    private Auction auction;
    private Item item;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp(){
        //Xóa sạch dữ liệu cũ
        bidHistoryRepository.deleteAll();
        itemRepository.deleteAll();
        auctionRepository.deleteAll();

        //Tạo và lưu người bán
        seller = new User();
        seller.setName("seller");
        seller.setBalance(1000.0);
        seller.setFreeze_balance(0.0);
        seller = userRepository.save(seller);

        //Tạo và lưu người đấu giá A
        bidderA = new User();
        bidderA.setName("Nguyễn Văn A");
        bidderA.setBalance(1000.0);
        bidderA.setFreeze_balance(0.0);
        bidderA = userRepository.save(bidderA);

        //Tạo và lưu người đấu giá B
        bidderB = new User();
        bidderB.setName("Trần Thị B");
        bidderB.setBalance(2000.0);
        bidderB.setFreeze_balance(0.0);
        bidderB = userRepository.save(bidderB);

        //Tạo và lưu item của phiên đấu giá
        item = new Vehicle();
        item.setName("xe đạp điện");
        item.setSeller(seller);
        item = itemRepository.save(item);

        // Tạo và lưu Phiên đấu giá mẫu (Đang mở)
        auction = new Auction();
        auction.setTitle("Sản phẩm đấu giá test");
        auction.setSeller(seller);
        auction.setItem(item);
        auction.setId(auction.getItem().getId());
        auction.setStatus(AuctionStatus.OPEN.toString());
        auction.setStartTime(LocalDateTime.now().minusHours(1)); // Đã bắt đầu 1 tiếng trước
        auction.setEndTime(LocalDateTime.now().plusHours(2));    // 2 tiếng nữa mới kết thúc
        auction.setCurrentPrice(500.0);
        auction.setBidIncrement(50.0); // Bước giá tối thiểu là 50
        auction = auctionRepository.save(auction);
    }

    ///Hàm tiện ích kích hoạt AfterCommit thủ công để test WebSocket thành công
    private void triggerAfterCommitCallbacks() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            for (TransactionSynchronization synchronization : synchronizations) {
                synchronization.afterCommit();
            }
        }
    }


    //BỘ TC KIỂM TRA ĐẶT BID THỦ CÔNG
    //Nhóm TC thành công
    @Test
    @DisplayName("Đặt giá hợp lệ lần đầu tiên - Thành công")
    void processPlaceBid_FirstValidBid_Success(){
        AuctionUpdateResponse response = bidService.ProcessPlaceBid(auction.getId(), bidderA.getID(), 600.0);

        //Kiểm tra dữ liệu trả về Response
        assertNotNull(response);
        assertEquals(600, response.getCurrentPrice());
        assertEquals(bidderA.getID(), response.getWinnerId());
        assertEquals(AuctionStatus.RUNNING.toString(), response.getAuctionStatus());

        //Kiểm tra ví tiền trong database H2 của bidderA
        User updatedBidderA = userRepository.findById(bidderA.getID()).orElseThrow();
        assertEquals(400.0, updatedBidderA.getBalance()); // 1000 - 600
        assertEquals(600.0, updatedBidderA.getFreeze_balance()); // Bị đóng băng 600

        //Ép Spring kích nổ sự kiện afterCommit() giả lập trong môi trường Test
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            for (TransactionSynchronization synchronization : synchronizations) {
                synchronization.afterCommit(); // Gọi bằng tay trong luồng test
            }
        }

        // Kiểm tra xem WebSocket có được gọi để gửi thông báo không
        Mockito.verify(simpMessagingTemplate, Mockito.atLeastOnce())
                .convertAndSend(eq("/topic/auction-" + auction.getId()), any(AuctionUpdateResponse.class));
    }

    @Test
    @DisplayName("Người thứ hai đặt giá cao hơn - Người cũ được hoàn tiền, người mới bị trừ tiền")
    void processPlaceBid_SecondValidBid_Success(){
        //Lượt 1: người A đặt 600
        bidService.ProcessPlaceBid(auction.getId(), bidderA.getID(), 600.0);

        //Lượt 2: người B đặt 700
        AuctionUpdateResponse response = bidService.ProcessPlaceBid(auction.getId(), bidderB.getID(), 700.0);

        //Kiểm tra phiên đấu giá cập nhật Winner chưa
        assertEquals(700.0, response.getCurrentPrice());
        assertEquals(bidderB.getID(), response.getWinnerId());

        //Kiểm tra hoàn tiền
        User updatedBidderA = userRepository.findById(bidderA.getID()).orElseThrow();
        assertEquals(1000.0, updatedBidderA.getBalance());
        assertEquals(0.0, updatedBidderA.getFreeze_balance());

        //Kiểm tra trừ tiền
        User updatedBidderB = userRepository.findById(bidderB.getID()).orElseThrow();
        assertEquals(1300.0, updatedBidderB.getBalance());
        assertEquals(700.0, updatedBidderB.getFreeze_balance());
    }

    @Test
    @DisplayName("Đặt giá trong 30 giây cuối - Thời gian kết thúc phải tự động gia hạn")
    void processPlaceBid_AntiSniping_ExtendsTime(){
        //Ép thời gian kết thúc phiên đấu giá về còn 10 giây
        LocalDateTime nearEndTime = LocalDateTime.now().plusSeconds(10);
        auction.setEndTime(nearEndTime);
        auctionRepository.save(auction);

        //người A đặt giá
        AuctionUpdateResponse response = bidService.ProcessPlaceBid(auction.getId(), bidderA.getID(), 600.0);

        //thời gian đóng phiên phải dài hơn thời gian cũ
        assertTrue(response.getEndTime().isAfter(nearEndTime));

        //Kiểm tra xem có đúng là gia hạn thêm 60s ko
        long duration = Duration.between(LocalDateTime.now(), response.getEndTime()).getSeconds();
        assertTrue(duration >= 58 && duration <= 61);
    }

    //Nhóm TC thất bại
    @Test
    @DisplayName("Chủ sản phẩm tự đặt giá")
    void processPlaceBid_SellerBidsOwnItem() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bidService.ProcessPlaceBid(auction.getId(), seller.getID(), 600.0);
        });

        assertEquals("Seller cannot bid on their own item.", exception.getMessage());
    }

    @Test
    @DisplayName("Người giữ giá cao nhât tự đặt giá đè lên chính mình")
    void processPlaceBid_WinnerBidsAgain(){
        bidService.ProcessPlaceBid(auction.getId(), bidderA.getID(), 600.0);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bidService.ProcessPlaceBid(auction.getId(), bidderA.getID(), 700.0);
        });

        assertEquals("you are holding the highest bid", exception.getMessage());
    }

    @Test
    @DisplayName("Đặt giá nhỏ hơn mức tối thiểu (Giá hiện tại + bước giá)")
    void processPlaceBid_BidAmountTooLow() {
        // Giá hiện tại 500, bước giá 50 -> Tối thiểu phải đặt 550
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bidService.ProcessPlaceBid(auction.getId(), bidderA.getId(), 540.0);
        });

        assertEquals("auction bid amount not enough", exception.getMessage());
    }

    @Test
    @DisplayName("Tài khoản người dùng không đủ tiền để trả mức giá đã đặt ")
    void processPlaceBid_NotEnoughBalance() {
        // Ví người A chỉ có 1000 nhưng đặt 1500
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bidService.ProcessPlaceBid(auction.getId(), bidderA.getId(), 1500.0);
        });

        assertEquals("user's account bid amount not enough", exception.getMessage());
    }

    @Test
    @DisplayName("Đặt giá khi phiên đấu giá đã kết thúc do hết giờ - Báo lỗi và cập nhật trạng thái")
    void processPlaceBid_AuctionEnded_ThrowsException() {
        // Ép thời gian kết thúc về quá khứ (đã hết giờ 10 phút trước)
        auction.setEndTime(LocalDateTime.now().minusMinutes(10));
        auctionRepository.save(auction);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bidService.ProcessPlaceBid(auction.getId(), bidderA.getId(), 600.0);
        });

        assertEquals("Auction has been closed", exception.getMessage());

        // Kiểm tra xem database H2 có tự động chuyển trạng thái đấu giá sang CANCELED vì không ai mua chưa
        Auction updatedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertEquals(AuctionStatus.CANCELED.toString(), updatedAuction.getStatus());
    }


    //BỘ TEST CASE KIỂM TRA AUTOBID

    @Test
    @DisplayName("Đăng ký auto-bid thành công và tự động kích nổ phát súng giá đầu tiên")
    void registerAutoBid_Success_AndTriggersFirstBid(){
        // Hành động: Bidder A đăng ký Auto-bid trần 1000.0 (Giá khởi điểm 500, bước giá 50)
        AuctionUpdateResponse response = bidService.registerAutoBid(auction.getId(), bidderA.getID(), 1000.0, 50.0);

        assertNotNull(response);
        assertEquals("Kích hoạt Auto-bid thành công", response.getMessage());

        //kiểm tra xem thực thể AutoBid đã lưu xuống db chưa
        boolean exists = autoBidRepository.findByAuctionAndUserAndActiveTrue(auction, bidderA).isPresent();
        assertTrue(exists);

        // Xác thực hệ thống tự kích nổ giá đầu tiên = startPrice + increment = 550.0
        Auction updatedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertEquals(550.0, updatedAuction.getCurrentPrice());
        assertEquals(bidderA.getId(), updatedAuction.getWinner().getId());
        assertEquals(AuctionStatus.RUNNING.toString(), updatedAuction.getStatus());

        // Xác thực số tiền đóng băng của Bidder A
        User updatedBidderA = userRepository.findById(bidderA.getId()).orElseThrow();
        assertEquals(450.0, updatedBidderA.getBalance());
        assertEquals(550.0, updatedBidderA.getFreeze_balance());

        // Xác thực tin nhắn Realtime được bắn đi sau khi commit
        triggerAfterCommitCallbacks();
        Mockito.verify(simpMessagingTemplate, Mockito.atLeastOnce()).convertAndSend(eq("/topic/auction-" + auction.getId()), any(AuctionUpdateResponse.class));
    }

    @Test
    @DisplayName("Đăng ký lỗi khi giá trần thấp hơn mức giá tối thiểu tiếp theo")
    void registerAutoBid_WhenMaxBidLessThanMinimum(){
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bidService.registerAutoBid(auction.getId(), bidderA.getID(), 540.0, 50.0);
        });

        assertTrue(exception.getMessage().contains("Maximum price must be greater than or equal: " + 550.0));
    }

    @Test
    @DisplayName("Đăng ký lỗi khi số dư tài khoản nhỏ hơn giá trần muốn thiết lập")
    void registerAutoBid_WhenBalanceInsufficient(){
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bidService.registerAutoBid(auction.getId(), bidderA.getID(), 1500.0, 50.0);
        });

        assertTrue(exception.getMessage().contains("The balance is not sufficient"));
    }

    @Test
    @DisplayName("Cuộc đua Auto-bid")
    void autoBidCompetition_TwoRobotsBiddingWar(){
        // Bidder A cài giá trần tối đa là 800.0
        bidService.registerAutoBid(auction.getId(), bidderA.getId(), 800.0, 50.0);

        // Ngay sau đó, Bidder B nhảy vào cài giá trần cao hơn hẳn: 1200.0
        // Hệ thống sẽ chạy hàm runAutoBidCompetition để hai bên liên tục đớp giá ngầm của nhau
        AuctionUpdateResponse response = bidService.registerAutoBid(auction.getId(), bidderB.getId(), 1200.0, 50.0);

        Auction finalAuction = auctionRepository.findById(auction.getId()).orElseThrow();

        // Hai con bot đặt giá đè lên nhau, khi B đặt 800 thì A ko thể đặt lên đc nữa
        assertEquals(800.0, finalAuction.getCurrentPrice());
        assertEquals(bidderB.getId(), finalAuction.getWinner().getId());

        // Kiểm tra tình trạng tài khoản của đấu thủ thua cuộc (Bidder A) -> Phải được hoàn lại toàn bộ tiền
        User finalBidderA = userRepository.findById(bidderA.getId()).orElseThrow();
        assertEquals(1000.0, finalBidderA.getBalance()); // Trả lại ví gốc
        assertEquals(0.0, finalBidderA.getFreeze_balance()); // Giải phóng đóng băng

        // Kiểm tra tình trạng tài khoản đấu thủ đang thắng (Bidder B) -> Đang tạm giữ 850.0
        User finalBidderB = userRepository.findById(bidderB.getId()).orElseThrow();
        assertEquals(2000.0 - 800.0, finalBidderB.getBalance());
        assertEquals(800.0, finalBidderB.getFreeze_balance());
    }

    @Test
    @DisplayName("Đấu giá thủ công kích vs auto-bid")
    void manualBid_TriggersAutoBidResponse(){
        // Đầu tiên, Bidder B cài cấu hình tự động với giá trần rất cao: 1500.0
        bidService.registerAutoBid(auction.getId(), bidderB.getId(), 1500.0, 50.0);
        // Lúc này giá phòng đấu sẽ tự kích lên mức đầu là 550.0, Winner là B.

        // Bây giờ, Bidder A cố tình nhảy vào đặt giá thủ công cao hơn: 700.0
        // Lệnh đặt giá thủ công này sẽ gọi runAutoBidCompetition ở cuối luồng để kích hoạt Robot của B phản pháo
        AuctionUpdateResponse manualBidResponse = bidService.ProcessPlaceBid(auction.getId(), bidderA.getId(), 700.0);

        Auction currentAuction = auctionRepository.findById(auction.getId()).orElseThrow();

        // KẾT QUẢ: Giá của A (700.0) vừa vào liền bị hệ thống tự động của B nâng lên đè bẹp ngay:
        // 700.0 + Bước giá 50.0 = 750.0.
        assertEquals(750.0, currentAuction.getCurrentPrice());
        assertEquals(bidderB.getId(), currentAuction.getWinner().getId()); // Thằng B vẫn vững vàng giữ Top 1
    }

    //BỘ TEST CASE KIỂM TRA REALTIME UPDATE
    @Test
    @DisplayName("Thành công: thông báo Real-time chỉ gửi sau khi Transaction đã Commit")
    void publicUpdate_ShouldSendNotification_AfterTransactionCommit(){
        // Lưu một lịch sử bid thật vào H2 Database thông qua @Autowired Repository
        BidHistory oldBid = new BidHistory();
        oldBid.setAuction(auction);
        oldBid.setUser(bidderA);
        oldBid.setBidAmount(1000.0);
        oldBid.setBidTime(LocalDateTime.now().minusMinutes(10));
        bidHistoryRepository.save(oldBid); // Lưu thật xuống H2

        //Cập nhận trạng thái hiện tại của auction
        auction.setWinner(bidderA);
        auction.setCurrentPrice(1000.0);
        auctionRepository.save(auction);

        //người nhận thông báo cá nhân sẽ là bidderA
        Long participantId = bidderA.getId();

        //BidderB đặt giá cao hơn
        AuctionUpdateResponse response = bidService.ProcessPlaceBid(auction.getId(), bidderB.getId(), 1200.0);

        assertNotNull(response);

        //Khẳng định: Trong khi transaction chưa commit, WebSocket không được phép gửi
        Mockito.verify(simpMessagingTemplate, Mockito.times(0)).convertAndSend(any(String.class), any(Object.class));

        triggerAfterCommitCallbacks();
        //Khẳng định: sau khi khối transactionTemplate commit, WebSocket phải gọi đúng và đủ các kênh(topic chung, topic item, topic cá nhân)
        Mockito.verify(simpMessagingTemplate).convertAndSend(eq("/topic/auction-" + auction.getId()), any(AuctionUpdateResponse.class));
        Mockito.verify(simpMessagingTemplate).convertAndSend(eq("/topic/items"), any(AuctionUpdateResponse.class));
        Mockito.verify(simpMessagingTemplate).convertAndSend(eq("/topic/user-" + participantId), any(AuctionUpdateResponse.class));
    }

    @Test
    @DisplayName("Lỗi hệ thống: Không gửi được thông báo Real-time nếu Transaction bị Rollback")
    void publicUpdate_ShouldNotSendNotification_IfTransactionIsRolledBack(){
        try{
            bidService.ProcessPlaceBid(auction.getId(), bidderA.getId(), 600.0);

            //ép buộc hệ thống xảy ra lỗi runtime hoặc chủ động đánh dấu Rollback nửa chừng
            throw new RuntimeException("Cố tình gây ra lỗi hệ thống để hủy giao dịch DB");
        }catch (RuntimeException e){}

        //Khẳng định: DB bị rollback, người dùng không nhận được thông tin sai lệch
        Mockito.verify(simpMessagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
    }
}
