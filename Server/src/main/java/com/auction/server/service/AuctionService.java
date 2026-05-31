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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionService {

    // Khởi tạo Logger SLF4J cho lớp AuctionService
    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final AuctionRepository auctionRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Autowired
    public AuctionService(SimpMessagingTemplate simpMessagingTemplate,
                          AuctionRepository auctionRepository,
                          NotificationRepository notificationRepository,
                          UserRepository userRepository) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.auctionRepository = auctionRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // Hàm xử lý logic khi phiên đấu giá kết thúc
    @Transactional
    public void endAuction(Long auctionId) {
        log.info("Bắt đầu xử lý kết thúc phiên đấu giá ID: {}", auctionId);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên với ID: " + auctionId));

        if (AuctionStatus.FINISHED.toString().equals(auction.getStatus()) ||
                AuctionStatus.CANCELED.toString().equals(auction.getStatus())) {
            log.warn("Hủy xử lý kết thúc: Phiên đấu giá ID {} đã ở trạng thái cuối ({})", auctionId, auction.getStatus());
            return;
        }

        User winner = auction.getWinner();
        User seller = auction.getSeller();

        if (winner == null) {
            // Trường hợp không có ai đặt giá
            log.info("Phiên đấu giá ID {} kết thúc không có người tham gia. Tiến hành hủy phiên.", auctionId);
            auction.setStatus(AuctionStatus.CANCELED.toString());
            auctionRepository.save(auction);
            sendAuctionStatusUpdate(auction, "Auction has been canceled");

            if (seller != null) {
                Notification sellerNotification = new Notification();
                sellerNotification.setUserId(seller.getId());
                sellerNotification.setAuctionId(auction.getId());
                sellerNotification.setType("AUCTION_CANCELED");
                sellerNotification.setMessage("Phiên đấu giá sản phẩm '" + auction.getItem().getName()
                        + "' của bạn đã kết thúc nhưng không có thành viên nào tham gia đặt giá.");
                sellerNotification.setHandled(true);

                notificationRepository.save(sellerNotification);
                sendNotificationViaSocket(seller.getId(), sellerNotification);
            }
        } else {
            // Trường hợp tìm được người đặt giá cao nhất
            log.info("Phiên đấu giá ID {} kết thúc thành công. Người thắng cuộc ID: {}, Giá chốt: {}",
                    auctionId, winner.getId(), auction.getCurrentPrice());

            auction.setStatus(AuctionStatus.FINISHED.toString());
            auctionRepository.save(auction);

            // Tạo và lưu thông báo cho người thắng cuộc
            Notification winnerNoti = new Notification();
            winnerNoti.setUserId(winner.getId());
            winnerNoti.setAuctionId(auctionId);
            winnerNoti.setType("WINNER_CONFIRM");
            winnerNoti.setMessage("Chúc mừng! Bạn đã thắng phiên đấu giá '" + auction.getItem().getName()
                    + "' với mức giá $" + String.format("%.2f", auction.getCurrentPrice())
                    + ". Vui lòng xác nhận thanh toán hoặc từ chối trong vòng 60 phút.");
            winnerNoti.setHandled(false);
            winnerNoti.setDeadline(LocalDateTime.now().plusMinutes(60));

            notificationRepository.save(winnerNoti);
            sendNotificationViaSocket(winner.getId(), winnerNoti);

            // Tạo và lưu thông báo cho người bán
            if (seller != null) {
                Notification sellerNoti = new Notification();
                sellerNoti.setUserId(seller.getId());
                sellerNoti.setAuctionId(auctionId);
                sellerNoti.setType("SELLER_AUCTION_ENDED");
                sellerNoti.setMessage("Phiên đấu giá sản phẩm '" + auction.getItem().getName()
                        + "' của bạn đã kết thúc thành công với giá $" + String.format("%.2f", auction.getCurrentPrice())
                        + ". Người thắng cuộc đang tiến hành thủ tục xác nhận thanh toán.");
                sellerNoti.setHandled(true);

                notificationRepository.save(sellerNoti);
                sendNotificationViaSocket(seller.getId(), sellerNoti);
            }
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.debug("Transaction committed. Sending REFRESH_SIGNAL to room /topic/auction-{}", auctionId);
                    simpMessagingTemplate.convertAndSend("/topic/auction-" + auctionId, "REFRESH_SIGNAL");
                }
            });
        } else {
            simpMessagingTemplate.convertAndSend("/topic/auction-" + auctionId, "REFRESH_SIGNAL");
        }
    }

    // Gửi thông báo cá nhân
    private void sendNotificationViaSocket(Long userId, Notification notification) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            String jsonMessage = mapper.writeValueAsString(notification);

            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.trace("Đang gửi thông báo WebSocket tới user-{}", userId);
                        simpMessagingTemplate.convertAndSend("/topic/user-" + userId + "/notifications", jsonMessage);
                    }
                });
            } else {
                simpMessagingTemplate.convertAndSend("/topic/user-" + userId + "/notifications", jsonMessage);
            }
        } catch (Exception e) {
            log.error("Lỗi đóng gói Object Notification sang JSON cho userId {}: ", userId, e);
        }
    }

    @Transactional
    public void handleWinnerConfirm(Long notificationId, boolean accept) {
        log.info("Xử lý phản hồi Winner Confirm - NotificationID: {}, Chấp nhận: {}", notificationId, accept);

        Notification noti = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông báo xác nhận với ID: " + notificationId));

        if (noti.isHandled()) {
            throw new IllegalStateException("Thông báo này đã được xử lý trước đó rồi.");
        }

        Auction auction = auctionRepository.findById(noti.getAuctionId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá liên quan ID: " + noti.getAuctionId()));

        if (!AuctionStatus.FINISHED.toString().equals(auction.getStatus())) {
            throw new IllegalStateException("Chỉ những phiên đã kết thúc (FINISHED) mới có thể thực hiện thanh toán.");
        }

        User winner = auction.getWinner();
        User seller = auction.getSeller();
        double finalPrice = auction.getCurrentPrice();

        if (winner == null) {
            throw new IllegalStateException("Phiên đấu giá kết thúc không có người thắng, không thể xử lý xác nhận.");
        }

        if (accept) {
            // TRƯỜNG HỢP 1: NGƯỜI THẮNG ĐỒNG Ý CHUYỂN TIỀN
            if (winner.getFreeze_balance() < finalPrice) {
                log.error("Thanh toán thất bại: Tài khoản đóng băng của winner ID {} không đủ số dư. Cần: {}, Có: {}",
                        winner.getId(), finalPrice, winner.getFreeze_balance());
                throw new IllegalStateException("Tài khoản đóng băng của người thắng không đủ số dư để thực hiện giao dịch.");
            }

            winner.setFreeze_balance(winner.getFreeze_balance() - finalPrice);
            userRepository.save(winner);

            if (seller != null) {
                seller.setBalance(seller.getBalance() + finalPrice);
                userRepository.save(seller);

                Notification sellerNoti = new Notification();
                sellerNoti.setUserId(seller.getId());
                sellerNoti.setAuctionId(auction.getId());
                sellerNoti.setType("SELLER_PAYMENT_SUCCESS");
                sellerNoti.setMessage("Tin vui! Người thắng phiên '" + auction.getItem().getName()
                        + "' đã xác nhận thanh toán. Số tiền $" + String.format("%.2f", finalPrice)
                        + " đã được cộng vào tài khoản khả dụng của bạn.");
                sellerNoti.setHandled(true);

                notificationRepository.save(sellerNoti);
                sendNotificationViaSocket(seller.getId(), sellerNoti);
            }

            auction.setStatus(AuctionStatus.PAID.toString());
            auctionRepository.save(auction);
            log.info("Thanh toán hoàn tất thành công cho phiên ID {}. Trạng thái cập nhật sang PAID.", auction.getId());

            sendAuctionStatusUpdate(auction, "Auction has been paid");
        } else {
            // TRƯỜNG HỢP 2: NGƯỜI THẮNG BẤM TỪ CHỐI (HỦY KÈO)
            log.warn("Người thắng (ID: {}) chủ động TỪ CHỐI thanh toán cho phiên đấu giá ID: {}", winner.getId(), auction.getId());

            if (winner.getFreeze_balance() >= finalPrice) {
                winner.setFreeze_balance(winner.getFreeze_balance() - finalPrice);
                winner.setBalance(winner.getBalance() + finalPrice);
                userRepository.save(winner);
            }

            if (seller != null) {
                Notification sellerCancelNoti = new Notification();
                sellerCancelNoti.setUserId(seller.getId());
                sellerCancelNoti.setAuctionId(auction.getId());
                sellerCancelNoti.setType("SELLER_BUYER_CANCELED");
                sellerCancelNoti.setMessage("Rất tiếc! Người thắng giải phiên đấu giá '" + auction.getItem().getName()
                        + "' đã từ chối xác nhận thanh toán (Hủy kèo). Bạn có thể mở lại phiên hoặc liên hệ quản trị viên.");
                sellerCancelNoti.setHandled(true);

                notificationRepository.save(sellerCancelNoti);
                sendNotificationViaSocket(seller.getId(), sellerCancelNoti);
            }

            auction.setStatus(AuctionStatus.CANCELED.toString());
            auctionRepository.save(auction);
        }

        noti.setHandled(true);
        notificationRepository.save(noti);
    }

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void autoCancelExpiredConfirmations() {
        LocalDateTime now = LocalDateTime.now();

        // Tìm tất cả các thông báo yêu cầu xác nhận mua chưa được xử lý và đã quá hạn
        List<Notification> expiredNotis = notificationRepository
                .findByTypeAndHandledFalseAndDeadlineBefore("WINNER_CONFIRM", now);

        if (expiredNotis.isEmpty()) {
            return;
        }

        log.info("[SERVER TASK] Phát hiện {} ca quá hạn xác nhận thanh toán. Bắt đầu tự động xử lý hủy kèo.", expiredNotis.size());

        for (Notification noti : expiredNotis) {
            try {
                Auction auction = auctionRepository.findById(noti.getAuctionId()).orElse(null);
                if (auction == null) {
                    log.warn("Task hủy kèo: Không tìm thấy phiên đấu giá ID {} liên quan tới thông báo ID {}", noti.getAuctionId(), noti.getId());
                    noti.setHandled(true);
                    notificationRepository.save(noti);
                    continue;
                }

                User winner = auction.getWinner();
                User seller = auction.getSeller();
                double finalPrice = auction.getCurrentPrice();

                // Trả lại tiền từ đóng băng về ví khả dụng cho người bùng cược
                if (winner != null && winner.getFreeze_balance() >= finalPrice) {
                    winner.setFreeze_balance(winner.getFreeze_balance() - finalPrice);
                    winner.setBalance(winner.getBalance() + finalPrice);
                    userRepository.save(winner);
                }

                // Gửi thông báo hết hạn cho người bán
                if (seller != null) {
                    Notification sellerTimeoutNoti = new Notification();
                    sellerTimeoutNoti.setUserId(seller.getId());
                    sellerTimeoutNoti.setAuctionId(auction.getId());
                    sellerTimeoutNoti.setType("SELLER_BUYER_TIMEOUT");
                    sellerTimeoutNoti.setMessage("Thông báo: Người thắng phiên '" + auction.getItem().getName()
                            + "' đã không xác nhận thanh toán sau 60 phút quy định. Phiên đấu giá bị hủy kết quả tự động.");
                    sellerTimeoutNoti.setHandled(true);

                    notificationRepository.save(sellerTimeoutNoti);
                    sendNotificationViaSocket(seller.getId(), sellerTimeoutNoti);
                }

                auction.setStatus(AuctionStatus.CANCELED.toString());
                auctionRepository.save(auction);

                noti.setHandled(true);
                notificationRepository.save(noti);

                log.info("Tự động hủy thành công do quá hạn thanh toán cho phiên ID: {}, Thông báo ID: {}", auction.getId(), noti.getId());
            } catch (Exception e) {
                log.error("Gặp lỗi nghiêm trọng khi tự động xử lý hủy thông báo hết hạn ID {}: ", noti.getId(), e);
            }
        }
    }

    @Transactional
    public void terminateAuction(Long auctionId) {
        log.info("Quản trị viên yêu cầu ép chấm dứt (TERMINATE) phiên đấu giá ID: {}", auctionId);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên với ID: " + auctionId));

        if (AuctionStatus.CANCELED.toString().equals(auction.getStatus()) ||
                AuctionStatus.PAID.toString().equals(auction.getStatus())) {
            log.warn("Không thể ép chấm dứt: Phiên ID {} đã kết thúc với trạng thái {}", auctionId, auction.getStatus());
            return;
        }

        // Hoàn trả tiền đóng băng cho Winner hiện tại nếu có
        User winner = auction.getWinner();
        if (winner != null) {
            double finalPrice = auction.getCurrentPrice();
            winner.setBalance(winner.getBalance() + finalPrice);
            winner.setFreeze_balance(Math.max(0, winner.getFreeze_balance() - finalPrice));
            userRepository.save(winner);

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

        simpMessagingTemplate.convertAndSend("/topic/auction-" + auctionId, "REFRESH_SIGNAL");
        log.info("Ép chấm dứt phiên ID {} hoàn tất. Đã gửi REFRESH_SIGNAL.", auctionId);
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

        log.debug("Phát thông điệp cập nhật trạng thái phiên ID {} lên hệ thống WebSocket", auction.getId());
        simpMessagingTemplate.convertAndSend("/topic/auction-" + auction.getId(), update);
        simpMessagingTemplate.convertAndSend("/topic/items", update);
    }
}