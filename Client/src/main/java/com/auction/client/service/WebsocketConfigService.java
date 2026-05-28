package com.auction.client.service;

import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;

import java.lang.reflect.Type;

public class WebsocketConfigService {
    private static WebsocketConfigService instance;
    @Getter
    @Setter
    private StompSession stompSession;
    private StompSession.Subscription currentAuctionSubscription;
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    private WebsocketConfigService() {}
    public static synchronized WebsocketConfigService getInstance() {
        if (instance == null) {
            instance = new WebsocketConfigService();
        }
        return instance;
    }

    public void connect(){
        if (stompSession != null && stompSession.isConnected()) {
            return;
        }
        String url = "ws://localhost:8080/ws"; // Thay bằng URL endpoint WebSocket bên Server của bạn

        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new StringMessageConverter()); // Định dạng text/string

        stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println("Kết nối WebSocket thành công rực rỡ!");
                stompSession = session;


                connectAndListenWebSocket();
            }

            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                System.err.println("Lỗi Socket: " + exception.getMessage());
                exception.printStackTrace();
            }
            @Override
            public void handleTransportError(StompSession session, Throwable throwable){
                System.out.println("Mất kết nối hoặc server sập: " + throwable.getMessage());
            }
        });
    }
    private void connectAndListenWebSocket(){
        Long curenntUserId = Session.getUser().getId();
        String topic = "/topic/user-" +curenntUserId;

        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = (String) payload;
                System.out.println(message);

                Platform.runLater(() -> {

                    switch (message){
                        case "ROLE_UPDATED_TO_SELLER":
                            AppEventBus.emit("SELLER_APPROVED", null);
                            break;
                        case "REGISTRATION_REJECTED":
                            AppEventBus.emit("SELLER_REJECTED", null);
                            break;
                    }

                });
            }
        });
    }
    public void disconnect() {
        try {
            // Khi logout ngắt kết nối chính, nhớ dọn dẹp luôn kênh auction nếu đang mở
            if (currentAuctionSubscription != null) {
                currentAuctionSubscription.unsubscribe();
                currentAuctionSubscription = null;
            }

            if (stompSession != null && stompSession.isConnected()) {
                stompSession.disconnect();
                System.out.println("[Socket] Đã ngắt kết nối WebSocket chủ động thành công.");
            } else {
                System.out.println("[Socket] Không có kết nối nào đang hoạt động để ngắt.");
            }
        } catch (Exception e) {
            System.err.println("[Socket] Lỗi xảy ra khi đóng kết nối: " + e.getMessage());
        } finally {
            stompSession = null;
        }
    }
    public void subscribeAuctionRoom(Long auctionId, Runnable onSignalReceived) {
        if (stompSession == null || !stompSession.isConnected()) return;

        if (currentAuctionSubscription != null) {
            currentAuctionSubscription.unsubscribe();
        }

        String auctionTopic = "/topic/auction-" + auctionId;

        currentAuctionSubscription = stompSession.subscribe(auctionTopic, new StompFrameHandler() {
            @Override
            public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
                return String.class; // Hứng kiểu String từ tiếng chuông Server
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = (String) payload;
                System.out.println("[Socket] Chuông phòng đấu giá reo: " + message);

                // Nếu đúng tiếng chuông báo làm mới, lập tức bảo UI gọi hàm load lại bảng lịch sử
                if ("REFRESH_SIGNAL".equals(message)) {
                    if (onSignalReceived != null) {
                        Platform.runLater(onSignalReceived);
                    }
                }
            }
        });
        System.out.println("[Socket] Đã bật chế độ nghe chuông phòng đấu giá: " + auctionTopic);
    }
    public void unsubscribeAuctionRoom() {
        if (currentAuctionSubscription != null) {
            currentAuctionSubscription.unsubscribe();
            currentAuctionSubscription = null;
            System.out.println("[Socket] Đã hủy lắng nghe phòng đấu giá chủ động thành công.");
        }
    }

}
