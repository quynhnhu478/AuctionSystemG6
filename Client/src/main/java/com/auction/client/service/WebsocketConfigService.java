package com.auction.client.service;

import javafx.application.Platform;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;

public class WebsocketConfigService {
    private static WebsocketConfigService instance;
    private StompSession stompSession;
    private WebsocketConfigService() {}
    public static synchronized WebsocketConfigService getInstance() {
        if (instance == null) {
            instance = new WebsocketConfigService();
        }
        return instance;
    }
    public StompSession getStompSession() {
        return stompSession;
    }
    public void setStompSession(StompSession stompSession) {}
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
                        default:
                            System.out.println("Loi nhan khong xac dinh" +message);
                            break;
                    }

                });
            }
        });

        // --- TOPIC 2: ĐĂNG KÝ MỚI - Nhận thông tin đấu giá Real-time ---
        String auctionTopic = "/topic/auctions";
        stompSession.subscribe(auctionTopic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String jsonPayload = (String) payload;
                Platform.runLater(() -> {
                    try {
                        AppEventBus.emit("AUCTION_PRICE_UPDATED", jsonPayload);
                        System.out.println("➔ Received new price data via WebSocket: " + jsonPayload);
                    } catch (Exception e) {
                        System.err.println("Error processing Auction data from WebSocket: " + e.getMessage());
                    }
                });
            }
        });
    }
    public void disconnect() {
        try {
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
}
