package com.auction.client.service;

import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.function.Consumer;

public class WebSocketClientService {
    private static WebSocketClientService instance;
    private StompSession stompSession;
    private Consumer<StompSession> onConnectCallback; // Giữ hành động đăng ký của trang Home

    private WebSocketClientService() {}

    public static synchronized WebSocketClientService getInstance() {
        if (instance == null) {
            instance = new WebSocketClientService();
        }
        return instance;
    }

    // Hàm mới: Giúp các Controller đăng ký việc muốn làm sau khi mạng thông suốt
    public void doOnConnect(Consumer<StompSession> callback) {
        if (stompSession != null && stompSession.isConnected()) {
            callback.accept(stompSession); // Nếu đã kết nối rồi thì chạy luôn
        } else {
            this.onConnectCallback = callback; // Nếu chưa, xếp hàng đợi ở đây
        }
    }

    public void connect() {
        if (stompSession != null && stompSession.isConnected()) {
            return;
        }

        String url = "ws://localhost:8080/ws";
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new StringMessageConverter());

        stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, org.springframework.messaging.simp.stomp.StompHeaders connectedHeaders) {
                stompSession = session;
                System.out.println(" Kích hoạt kết nối WebSocket thành công tới Server!");

                // Khi kết nối thành công, tự động gọi luồng xử lý của trang Home đang đợi
                if (onConnectCallback != null) {
                    onConnectCallback.accept(stompSession);
                    onConnectCallback = null; // Chạy xong thì xóa đi để tránh trùng lặp
                }
            }
        });
    }

    public StompSession getStompSession() {
        return stompSession;
    }
}