package com.auction.client.service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import java.util.function.Consumer;
import java.lang.reflect.Type;

public class WebsocketConfigService {
    private static WebsocketConfigService instance;
    @Getter
    @Setter
    private StompSession stompSession;
    private final Map<Long, StompSession.Subscription> auctionSubscriptions = new HashMap<>();
    private final Map<Long, List<Consumer<String>>> auctionListeners = new HashMap<>();
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    private final List<Consumer<String>> itemListeners = new ArrayList<>();
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
                ensureItemSubscription();
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
        //Kênh trạng thái tài khoản
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
                    if (message != null && message.trim().startsWith("{")) {
                        AppEventBus.emit("USER_AUCTION_UPDATED", message);
                        return;
                    }

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

        // Kênh lắng nghe thông báo REAL-TIME gửi về cho chuông
        // Đường dẫn khớp với Server: /topic/user-{userId}/notifications
        String notificationTopic = "/topic/user-" + curenntUserId + "/notifications";
        stompSession.subscribe(notificationTopic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class; // Nhận payload từ Server là chuỗi JSON thô
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String jsonMessage = (String) payload;
                System.out.println("[Socket] Nhận thông báo mới: " + jsonMessage);

                // Bắn sự kiện kèm dữ liệu JSON sang cho MainLayoutController thông qua EventBus
                Platform.runLater(() -> {
                    AppEventBus.emit("NEW_NOTIFICATION_RECEIVED", jsonMessage);
                });
            }
        });
    }


    public void subscribeItems(Consumer<String> listener) {
        if (!itemListeners.contains(listener)) {
            itemListeners.add(listener);
        }
        ensureItemSubscription();
    }
    private void ensureItemSubscription() {
        if (stompSession == null || !stompSession.isConnected()) return;
        if (itemSubscription != null) return;

        itemSubscription = stompSession.subscribe("/topic/items", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = (String) payload;
                Platform.runLater(() -> {
                    for (Consumer<String> l : new ArrayList<>(itemListeners)) {
                        l.accept(message);
                    }
                });
            }
        });
    }


    public void disconnect() {
        try {
            for (StompSession.Subscription subscription : auctionSubscriptions.values()) {
                if (subscription != null) {
                    subscription.unsubscribe();
                }
            }
            auctionSubscriptions.clear();
            auctionListeners.clear();

            if (stompSession != null && stompSession.isConnected()) {
                stompSession.disconnect();
                System.out.println("[Socket] Disconnected WebSocket successfully.");
            } else {
                System.out.println("[Socket] No active WebSocket connection to disconnect.");
            }
        } catch (Exception e) {
            System.err.println("[Socket] Error while disconnecting: " + e.getMessage());
        } finally {
            stompSession = null;
        }
    }

    public void subscribeAuctionRoom(Long auctionId, Consumer<String> onMessageReceived) {
        if (stompSession == null || !stompSession.isConnected() || auctionId == null) return;

        auctionListeners.computeIfAbsent(auctionId, ignored -> new ArrayList<>()).add(onMessageReceived);

        if (auctionSubscriptions.containsKey(auctionId)) {
            return;
        }

        String auctionTopic = "/topic/auction-" + auctionId;

        StompSession.Subscription subscription = stompSession.subscribe(auctionTopic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = (String) payload;
                System.out.println("[Socket] Auction room message: " + message);

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

        auctionSubscriptions.put(auctionId, subscription);
    }


    public void unsubscribeAuctionRoom(Long auctionId, Consumer<String> listener) {
        if (auctionId == null || listener == null) {
            return;
        }

        List<Consumer<String>> listeners = auctionListeners.get(auctionId);
        if (listeners == null) {
            return;
        }

        listeners.remove(listener);
        if (listeners.isEmpty()) {
            auctionListeners.remove(auctionId);
            StompSession.Subscription subscription = auctionSubscriptions.remove(auctionId);
            if (subscription != null) {
                subscription.unsubscribe();
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
        // Không unsubscribe toàn bộ auction room ở đây nữa,
        // vì ProductCard vẫn cần nghe để cập nhật Number of bids.
    }

}
