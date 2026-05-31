package com.auction.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.auction.client.config.ApiConfig;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;
import java.util.function.Consumer;
import java.lang.reflect.Type;

public class WebsocketConfigService {
    // Khởi tạo hệ thống Log cho class
    private static final Logger log = LoggerFactory.getLogger(WebsocketConfigService.class);

    private static WebsocketConfigService instance;
    @Getter
    @Setter
    private StompSession stompSession;
    private final Map<Long, StompSession.Subscription> auctionSubscriptions = new HashMap<>();
    private final Map<Long, List<Consumer<String>>> auctionListeners = new HashMap<>();
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    private final List<Consumer<String>> itemListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private StompSession.Subscription itemSubscription;

    private WebsocketConfigService() {}

    public static synchronized WebsocketConfigService getInstance() {
        if (instance == null) {
            instance = new WebsocketConfigService();
        }
        return instance;
    }

    public void connect(){
        if (stompSession != null && stompSession.isConnected()) {
            log.debug("Hủy yêu cầu kết nối: WebSocket đã được kết nối trước đó.");
            return;
        }
        String url = ApiConfig.BASE_URL.replaceFirst("^http", "ws") + "/ws";
        log.info("Đang khởi tạo kết nối WebSocket tới: {}", url);

        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);

        // Thay ByteArrayMessageConverter bằng StringMessageConverter để xử lý chuỗi văn bản JSON UTF-8 chuẩn
        stompClient.setMessageConverter(new StringMessageConverter());

        stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                log.info("Kết nối WebSocket thành công rực rỡ! SessionID: {}", session.getSessionId());
                stompSession = session;

                connectAndListenWebSocket();
                ensureItemSubscription();
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                log.error("Lỗi giao thức STOMP Socket - Command: {}, Khởi phát lỗi: ", command, exception);
            }

            @Override
            public void handleTransportError(StompSession session, Throwable throwable){
                log.warn("Mất kết nối Transport hoặc server WebSocket sập: {}", throwable.getMessage());
            }
        });
    }

    private void connectAndListenWebSocket(){
        if (Session.getUser() == null) {
            log.warn("Không thể đăng ký topic cá nhân: Session user đang bị null.");
            return;
        }
        Long currentUserId = Session.getUser().getId();
        String topic = "/topic/user-" + currentUserId;
        log.info("Đang tiến hành subscribe topic cá nhân: {}", topic);

        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = (String) payload;
                log.debug("Nhận frame từ User Topic [{}]: {}", topic, message);

                Platform.runLater(() -> {
                    if (message != null && message.trim().startsWith("{")) {
                        AppEventBus.emit("USER_AUCTION_UPDATED", message);
                        return;
                    }

                    if (message != null) {
                        switch (message){
                            case "ROLE_UPDATED_TO_SELLER":
                                log.info("Nhận tín hiệu: Duyệt quyền Seller thành công.");
                                AppEventBus.emit("SELLER_APPROVED", null);
                                break;
                            case "REGISTRATION_REJECTED":
                                log.info("Nhận tín hiệu: Từ chối quyền Seller.");
                                AppEventBus.emit("SELLER_REJECTED", null);
                                break;
                            default:
                                log.trace("Tín hiệu text không xác định: {}", message);
                        }
                    }
                });
            }
        });

        String notificationTopic = "/topic/user-" + currentUserId + "/notifications";
        log.info("Đang tiến hành subscribe topic thông báo: {}", notificationTopic);
        stompSession.subscribe(notificationTopic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String jsonMessage = (String) payload;
                log.debug("[NOTIFICATION SOCKET] Nhận thông báo Real-time: {}", jsonMessage);

                Platform.runLater(() -> {
                    AppEventBus.emit("NEW_NOTIFICATION_RECEIVED", jsonMessage);
                });
            }
        });
    }

    public void subscribeItems(Consumer<String> listener) {
        if (!itemListeners.contains(listener)) {
            itemListeners.add(listener);
            log.debug("Đã thêm một listener mới vào danh sách itemListeners.");
        }
        ensureItemSubscription();
    }

    private void ensureItemSubscription() {
        if (stompSession == null || !stompSession.isConnected()) {
            log.warn("Không thể subscribe /topic/items: WebSocket chưa sẵn sàng.");
            return;
        }
        if (itemSubscription != null) return;

        log.info("Đang tiến hành đăng ký lắng nghe kênh danh sách sản phẩm (/topic/items)");
        itemSubscription = stompSession.subscribe("/topic/items", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = (String) payload;
                log.trace("Nhận cập nhật sản phẩm mới: {}", message);

                Platform.runLater(() -> {
                    for (Consumer<String> l : new ArrayList<>(itemListeners)) {
                        l.accept(message);
                    }
                });
            }
        });
    }

    public void disconnect() {
        log.info("Yêu cầu ngắt kết nối hệ thống WebSocket chủ động...");
        try {
            for (StompSession.Subscription subscription : auctionSubscriptions.values()) {
                if (subscription != null) {
                    subscription.unsubscribe();
                }
            }
            auctionSubscriptions.clear();
            auctionListeners.clear();

            if (itemSubscription != null) {
                itemSubscription.unsubscribe();
                itemSubscription = null;
            }
            itemListeners.clear();

            if (stompSession != null && stompSession.isConnected()) {
                stompSession.disconnect();
                log.info("[Socket] Đã chủ động ngắt kết nối WebSocket thành công.");
            } else {
                log.debug("[Socket] Không có kết nối WebSocket nào đang chạy để ngắt.");
            }
        } catch (Exception e) {
            log.error("[Socket] Gặp lỗi khi đang ngắt kết nối WebSocket: ", e);
        } finally {
            stompSession = null;
        }
    }

    public void subscribeAuctionRoom(Long auctionId, Consumer<String> onMessageReceived) {
        if (stompSession == null || !stompSession.isConnected() || auctionId == null) {
            log.warn("Không thể đăng ký phòng đấu giá {}: WebSocket chưa kết nối hoặc ID sai.", auctionId);
            return;
        }

        auctionListeners.computeIfAbsent(auctionId, ignored -> new ArrayList<>()).add(onMessageReceived);

        if (auctionSubscriptions.containsKey(auctionId)) {
            log.debug("Đã tồn tại Subscription cho phòng đấu giá ID: {}", auctionId);
            return;
        }

        String auctionTopic = "/topic/auction-" + auctionId;
        log.info("Đang tiến hành kết nối vào phòng đấu giá: {}", auctionTopic);

        StompSession.Subscription sub = stompSession.subscribe(auctionTopic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = (String) payload;
                log.debug("[Socket] Tin nhắn từ phòng đấu giá [{}]: {}", auctionId, message);

                List<Consumer<String>> listeners = auctionListeners.get(auctionId);
                if (listeners != null) {
                    Platform.runLater(() -> {
                        for (Consumer<String> listener : new ArrayList<>(listeners)) {
                            if (listener != null) {
                                listener.accept(message);
                            }
                        }
                    });
                }
            }
        });

        auctionSubscriptions.put(auctionId, sub);
    }

    public void unsubscribeAuctionRoom(Long auctionId, Consumer<String> listener) {
        if (auctionId == null || listener == null) return;

        List<Consumer<String>> listeners = auctionListeners.get(auctionId);
        if (listeners == null) return;

        listeners.remove(listener);
        log.debug("Đã gỡ bỏ 1 listener ra khỏi phòng đấu giá ID: {}", auctionId);

        if (listeners.isEmpty()) {
            auctionListeners.remove(auctionId);
            StompSession.Subscription subscription = auctionSubscriptions.remove(auctionId);
            if (subscription != null) {
                subscription.unsubscribe();
                log.info("Thoát hoàn toàn (Unsubscribed) khỏi phòng đấu giá ID: {}", auctionId);
            }
        }
    }

    public void subscribeAuctionRoom(Long auctionId, Runnable onSignalReceived) {
        subscribeAuctionRoom(auctionId, message -> {
            if (onSignalReceived != null) {
                onSignalReceived.run();
            }
        });
    }

    public void unsubscribeAuctionRoom() {
    }
}