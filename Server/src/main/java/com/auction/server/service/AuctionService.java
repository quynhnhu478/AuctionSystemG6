package com.auction.server.service;

import com.auction.common.enums.AuctionStatus;
import com.auction.common.payload.AuctionUpdateResponse;
import com.auction.server.model.Auction;
import com.auction.server.model.Notification;
import com.auction.server.model.user.User;
import com.auction.server.repository.AuctionRepository;
import com.auction.server.repository.NotificationRepository;
import com.auction.server.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuctionService {
    SimpMessagingTemplate simpMessagingTemplate;
    AuctionRepository auctionRepository;
    NotificationRepository notificationRepository;
    UserRepository userRepository;

    @Autowired
    public AuctionService(SimpMessagingTemplate simpMessagingTemplate, AuctionRepository auctionRepository, NotificationRepository notificationRepository, UserRepository userRepository) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.auctionRepository = auctionRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }


    //hàm xử lý logic khi phiên đấu giá kết thúc
    @Transactional
    public void endAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên"));

        if (AuctionStatus.FINISHED.toString().equals(auction.getStatus()) ||
                AuctionStatus.CANCELED.toString().equals(auction.getStatus())) {
            return;
        }

        User winner = auction.getWinner();
        User seller = auction.getSeller();
        if (winner == null) {
            // Trường hợp không có ai đặt giá
            auction.setStatus(AuctionStatus.CANCELED.toString());
            auctionRepository.save(auction);
            sendAuctionStatusUpdate(auction, "Auction has been canceled");
            if (seller != null) {
                Notification sellerNotification = new Notification();
                sellerNotification.setUserId(seller.getId());
                sellerNotification.setAuctionId(auction.getId());
                sellerNotification.setType("AUCTION_CANCELED");  //Định dang Type để client nhận biết
                sellerNotification.setMessage("Phiên đấu giá sản phẩm '" + auction.getItem().getName()
                + "' của bạn đã kết thúc nhưng khng có thành viên nào tham gia đă giá.");
                sellerNotification.setHandled(true);

                notificationRepository.save(sellerNotification);

                sendNotificationViaSocket(seller.getId(),  sellerNotification);
            }
        }
        else {
            // Trường hợp tìm được người đặt giá cao nhất
            auction.setStatus(AuctionStatus.FINISHED.toString());
            auctionRepository.save(auction);
            // THAY THẾ/SỬA ĐỔI TẠI ĐÂY: TẠO VÀ LƯU THÔNG BÁO CHO NGƯỜI THẮNG CUỘC
            Notification winnerNoti = new Notification();
            winnerNoti.setUserId(winner.getId());
            winnerNoti.setAuctionId(auctionId);
            winnerNoti.setType("WINNER_CONFIRM"); // Đổi type thành WINNER_CONFIRM để Client biết đường render 2 nút
            winnerNoti.setMessage("Chúc mừng! Bạn đã thắng phiên đấu giá '" + auction.getItem().getName()
                    + "' với mức giá $" + String.format("%.2f", auction.getCurrentPrice())
                    + ". Vui lòng xác nhận thanh toán hoặc từ chối trong vòng 60 phút.");
            winnerNoti.setHandled(false); // Chưa xử lý -> Để hiển thị nút bấm
            winnerNoti.setDeadline(LocalDateTime.now().plusMinutes(60)); // Cài deadline 60 phút cho @Scheduled quét

            // Lưu vào Database trước để Client có lịch sử đọc
            notificationRepository.save(winnerNoti);

            // Phát tín hiệu WebSocket real-time chứa toàn bộ Object thông báo cho Người thắng
            sendNotificationViaSocket(winner.getId(), winnerNoti);

            // THAY THẾ/SỬA ĐỔI TẠI ĐÂY: TẠO VÀ LƯU THÔNG BÁO CHO NGƯỜI BÁN
            if (seller != null) {
                Notification sellerNoti = new Notification();
                sellerNoti.setUserId(seller.getId());
                sellerNoti.setAuctionId(auctionId);
                sellerNoti.setType("SELLER_AUCTION_ENDED");
                sellerNoti.setMessage("Phiên đấu giá sản phẩm '" + auction.getItem().getName()
                        + "' của bạn đã kết thúc thành công với giá $" + String.format("%.2f", auction.getCurrentPrice())
                        + ". Người thắng cuộc đang tiến hành thủ tục xác nhận thanh toán.");
                sellerNoti.setHandled(true); // Tin báo chỉ đọc, không cần nút bấm

                notificationRepository.save(sellerNoti);

                // Phát tín hiệu WebSocket real-time chứa toàn bộ Object thông báo cho Người bán
                sendNotificationViaSocket(seller.getId(), sellerNoti);
            }
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Bắn tín hiệu làm mới đến toàn bộ phòng đấu giá cụ thể này
                    simpMessagingTemplate.convertAndSend("/topic/auction-" + auctionId, "REFRESH_SIGNAL");
                }
            });
        } else {
            simpMessagingTemplate.convertAndSend("/topic/auction-" + auctionId, "REFRESH_SIGNAL");
        }
    }


    //gửi thông báo cá nhân
    private void sendNotificationViaSocket(Long userId, Notification notification) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Đăng ký thêm JavaTimeModule nếu trường deadline/createdAt là LocalDateTime để tránh lỗi Format chuỗi
            mapper.registerModule(new JavaTimeModule());

            String jsonMessage = mapper.writeValueAsString(notification);

            // Đảm bảo bắn tín hiệu sau khi Database Transaction đã commit hoàn tất thành công
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        simpMessagingTemplate.convertAndSend("/topic/user-" + userId + "/notifications", jsonMessage);
                    }
                });
            } else {
                simpMessagingTemplate.convertAndSend("/topic/user-" + userId + "/notifications", jsonMessage);
            }
        } catch (Exception e) {
            System.err.println("Lỗi đóng gói Object Notification sang JSON: " + e.getMessage());
        }
    }

    @Transactional
    public void handleWinnerConfirm(Long notificationId, boolean accept) {
        // 1. Kiểm tra thông báo có tồn tại hay không
        Notification noti = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo xác nhận"));

        if (noti.isHandled()) {
            throw new IllegalStateException("Thông báo này đã được xử lý trước đó rồi.");
        }

        // 2. Lấy thông tin phiên đấu giá liên quan
        Auction auction = auctionRepository.findById(noti.getAuctionId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá liên quan"));
        if (!AuctionStatus.FINISHED.toString().equals(auction.getStatus())) {
            throw new IllegalStateException("Only finished auctions can be paid.");
        }
        User winner = auction.getWinner();
        User seller = auction.getSeller();
        double finalPrice = auction.getCurrentPrice();

        // Kiểm tra tính hợp lệ của dữ liệu người thắng cuộc
        if (winner == null) {
            throw new IllegalStateException("Phiên đấu giá kết thúc không có người thắng, không thể xử lý xác nhận.");
        }

        if (accept) {
            // TRƯỜNG HỢP 1: NGƯỜI THẮNG ĐỒNG Ý CHUYỂN TIỀN (XÁC NHẬN MUA)

            // 1. Trừ tiền thực tế ở ví đóng băng của người thắng
            if (winner.getFreeze_balance() < finalPrice) {
                throw new IllegalStateException("Tài khoản đóng băng của người thắng không đủ số dư để thực hiện giao dịch.");
            }
            winner.setFreeze_balance(winner.getFreeze_balance() - finalPrice);
            userRepository.save(winner);

            // 2. Cộng tiền thực tế vào tài khoản của người bán
            if (seller != null) {
                seller.setBalance(seller.getBalance() + finalPrice);
                userRepository.save(seller);

                // 3. Tạo thông báo mới gửi cho người bán để báo tin vui
                Notification sellerNoti = new Notification();
                sellerNoti.setUserId(seller.getId());
                sellerNoti.setAuctionId(auction.getId());
                sellerNoti.setType("SELLER_PAYMENT_SUCCESS");
                sellerNoti.setMessage("Tin vui! Người thắng phiên '" + auction.getItem().getName()
                        + "' đã xác nhận thanh toán. Số tiền $" + String.format("%.2f", finalPrice)
                        + " đã được cộng vào tài khoản khả dụng của bạn.");
                sellerNoti.setHandled(true); // Thông báo dạng đọc tin vui, không cần nút bấm xử lý nên set handled = true luôn

                notificationRepository.save(sellerNoti);
            }

            // Cập nhật trạng thái thanh toán hoặc trạng thái phụ của phiên nếu hệ thống của bạn yêu cầu (ví dụ: COMPLETED)
            auction.setStatus(AuctionStatus.PAID.toString());
            auctionRepository.save(auction);

            sendAuctionStatusUpdate(auction, "Auction has been paid");

        } else {
            // TRƯỜNG HỢP 2: NGƯỜI THẮNG BẤM TỪ CHỐI (HỦY KÈO / BÙNG CƠ HỘI)


            // 1. Hoàn lại tiền đóng băng về ví khả dụng cho họ (Hệ thống trả lại tiền tự do)
            if (winner.getFreeze_balance() >= finalPrice) {
                winner.setFreeze_balance(winner.getFreeze_balance() - finalPrice);
                winner.setBalance(winner.getBalance() + finalPrice);
                userRepository.save(winner);
            }

            // 2. Báo cho người bán biết là người thắng đã hủy kèo không chuyển tiền
            if (seller != null) {
                Notification sellerCancelNoti = new Notification();
                sellerCancelNoti.setUserId(seller.getId());
                sellerCancelNoti.setAuctionId(auction.getId());
                sellerCancelNoti.setType("SELLER_BUYER_CANCELED");
                sellerCancelNoti.setMessage("Rất tiếc! Người thắng giải phiên đấu giá '" + auction.getItem().getName()
                        + "' đã từ chối xác nhận thanh toán (Hủy kèo). Bạn có thể mở lại phiên hoặc liên hệ quản trị viên.");
                sellerCancelNoti.setHandled(true); // Chỉ gửi thông tin cảnh báo, không cần xử lý tiếp

                notificationRepository.save(sellerCancelNoti);
            }

            // Cập nhật trạng thái hủy thanh toán của phiên
            auction.setStatus(AuctionStatus.CANCELED.toString());
            auctionRepository.save(auction);
        }

        // 3. Đánh dấu thông báo WINNER_CONFIRM này đã giải quyết xong để ẩn nút trên UI của người thắng
        noti.setHandled(true);
        notificationRepository.save(noti);
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void autoCancelExpiredConfirmations() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Tìm tất cả các thông báo yêu cầu xác nhận mua chưa được xử lý và đã quá hạn (deadline < now)
        List<Notification> expiredNotis = notificationRepository
                .findByTypeAndHandledFalseAndDeadlineBefore("WINNER_CONFIRM", now);

        if (expiredNotis.isEmpty()) {
            return; // Không có ca nào quá hạn thì thoát sớm cho nhẹ máy
        }

        System.out.println("====== [SERVER TASK] Phát hiện " + expiredNotis.size() + " ca quá hạn thanh toán. Tiến hành tự động hủy kèo!");

        for (Notification noti : expiredNotis) {
            try {
                Auction auction = auctionRepository.findById(noti.getAuctionId()).orElse(null);
                if (auction == null) {
                    noti.setHandled(true);
                    notificationRepository.save(noti);
                    continue;
                }

                User winner = auction.getWinner();
                User seller = auction.getSeller();
                double finalPrice = auction.getCurrentPrice();

                // 2. Xử lý tài chính: Trả lại tiền từ đóng băng về ví khả dụng cho người bùng cược
                if (winner != null && winner.getFreeze_balance() >= finalPrice) {
                    winner.setFreeze_balance(winner.getFreeze_balance() - finalPrice);
                    winner.setBalance(winner.getBalance() + finalPrice);
                    userRepository.save(winner);

                    // (Tùy chọn nâng cao): Bạn có thể trừ phí phạt bùng kèo của winner tại đây nếu muốn
                    // winner.setBalance(winner.getBalance() - 10); // Phạt 10$ chẳng hạn
                }

                // 3. Gửi thông báo "Chia buồn bùng kèo do quá hạn" cho người bán
                if (seller != null) {
                    Notification sellerTimeoutNoti = new Notification();
                    sellerTimeoutNoti.setUserId(seller.getId());
                    sellerTimeoutNoti.setAuctionId(auction.getId());
                    sellerTimeoutNoti.setType("SELLER_BUYER_TIMEOUT");
                    sellerTimeoutNoti.setMessage("Thông báo: Người thắng phiên '" + auction.getItem().getName()
                            + "' đã không xác nhận thanh toán sau 60 phút quy định. Phiên đấu giá bị hủy kết quả tự động.");
                    sellerTimeoutNoti.setHandled(true);

                    notificationRepository.save(sellerTimeoutNoti);
                }

                // 4. Cập nhật trạng thái thanh toán của phiên đấu giá trong DB
                auction.setStatus("CANCELED");
                auctionRepository.save(auction);

                // 5. Đánh dấu thông báo cũ này đã giải quyết để không bị quét lại ở vòng sau
                noti.setHandled(true);
                notificationRepository.save(noti);

            } catch (Exception e) {
                System.err.println("Lỗi khi tự động hủy thông báo ID " + noti.getId() + ": " + e.getMessage());
            }
        }
    }
    @Transactional
    public void terminateAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên"));

        if (AuctionStatus.CANCELED.toString().equals(auction.getStatus()) ||
                AuctionStatus.PAID.toString().equals(auction.getStatus())) {
            return;
        }

        // Hoàn trả tiền đóng băng cho Winner hiện tại nếu có
        User winner = auction.getWinner();
        if (winner != null) {
            double finalPrice = auction.getCurrentPrice();
            winner.setBalance(winner.getBalance() + finalPrice);
            winner.setFreeze_balance(Math.max(0, winner.getFreeze_balance() - finalPrice));
            userRepository.save(winner);

            // Tạo thông báo cho Winner
            Notification winnerNoti = new Notification();
            winnerNoti.setUserId(winner.getId());
            winnerNoti.setAuctionId(auction.getId());
            winnerNoti.setType("AUCTION_TERMINATED");
            winnerNoti.setMessage("Phiên đấu giá '" + auction.getItem().getName()
                    + "' đã bị quản trị viên hủy bỏ (terminated). Số tiền đóng băng $" 
                    + String.format("%.2f", finalPrice) + " đã được hoàn lại vào tài khoản khả dụng của bạn.");
            winnerNoti.setHandled(true);
            notificationRepository.save(winnerNoti);
            sendNotificationViaSocket(winner.getId(), winnerNoti);
        }

        // Tạo thông báo cho Seller
        User seller = auction.getSeller();
        if (seller != null) {
            Notification sellerNoti = new Notification();
            sellerNoti.setUserId(seller.getId());
            sellerNoti.setAuctionId(auction.getId());
            sellerNoti.setType("AUCTION_TERMINATED");
            sellerNoti.setMessage("Phiên đấu giá sản phẩm '" + auction.getItem().getName()
                    + "' của bạn đã bị quản trị viên chấm dứt (terminated) do vi phạm chính sách.");
            sellerNoti.setHandled(true);
            notificationRepository.save(sellerNoti);
            sendNotificationViaSocket(seller.getId(), sellerNoti);
        }

        auction.setStatus(AuctionStatus.CANCELED.toString());
        auctionRepository.save(auction);

        sendAuctionStatusUpdate(auction, "Auction has been terminated by Administrator");
        
        // Phát tín hiệu làm mới đến toàn bộ phòng
        simpMessagingTemplate.convertAndSend("/topic/auction-" + auctionId, "REFRESH_SIGNAL");
    }

    private void sendAuctionStatusUpdate(Auction auction, String message) {
        AuctionUpdateResponse update = new AuctionUpdateResponse();
        update.setItemId(auction.getItem().getId());
        update.setAuctionId(auction.getId());
        update.setAuctionStatus(auction.getStatus());
        update.setCurrentPrice(auction.getCurrentPrice());
        update.setEndTime(auction.getEndTime());
        update.setServerTime(LocalDateTime.now());
        update.setMessage(message);

        if (auction.getBidHistories() != null) {
            update.setBidCount(auction.getBidHistories().size());
        }

        simpMessagingTemplate.convertAndSend("/topic/auction-" + auction.getId(), update);
        simpMessagingTemplate.convertAndSend("/topic/items", update);
    }
}
