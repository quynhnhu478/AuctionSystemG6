package com.auction.client.service;

import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.messaging.converter.StringMessageConverter;

public class WebSocketClientService {
    private static WebSocketClientService instance;
    private StompSession stompSession;

    private WebSocketClientService() {}

    public static synchronized WebSocketClientService getInstance() {
        if (instance == null) {
            instance = new WebSocketClientService();
        }
        return instance;
    }

    // Hàm gọi để kích hoạt kết nối tới Server
    public void connect() {
        if (stompSession != null && stompSession.isConnected()) {
            return; // Đã kết nối rồi thì không làm gì cả
        }

        String url = "ws://localhost:8080/ws"; // Đường dẫn endpoint đã cấu hình ở Server
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);

        // Kích hoạt bộ converter dạng String chuẩn để không bị lỗi Deprecated (Gạch đỏ)
        stompClient.setMessageConverter(new StringMessageConverter());

        stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, org.springframework.messaging.simp.stomp.StompHeaders connectedHeaders) {
                stompSession = session;
                System.out.println(" Kích hoạt kết nối WebSocket thành công tới Server!");
            }
        });
    }

    public StompSession getStompSession() {
        return stompSession;
    }
}