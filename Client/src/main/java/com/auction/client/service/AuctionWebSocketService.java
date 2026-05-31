/*package com.auction.client.service;

import javafx.application.Platform;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class AuctionWebSocketService {
    private static final String URL = "ws://localhost:8080/ws-auction";
    private static final AuctionWebSocketService INSTANCE = new AuctionWebSocketService();

    private final WebSocketStompClient stompClient;
    private final Map<String, List<Consumer<String>>> listeners = new ConcurrentHashMap<>();
    private final Set<String> subscribedTopics = ConcurrentHashMap.newKeySet();
    private volatile StompSession session;
    private volatile boolean connecting;

    private AuctionWebSocketService() {
        WebSocketClient client = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new StringMessageConverter());
    }

    public static AuctionWebSocketService getInstance() {
        return INSTANCE;
    }

    public void subscribe(String topic, Consumer<String> listener) {
        listeners.computeIfAbsent(topic, ignored -> new CopyOnWriteArrayList<>()).add(listener);
        ensureConnected();
        if (session != null && session.isConnected()) {
            subscribeTopicIfNeeded(topic);
        }
    }

    public void unsubscribe(String topic, Consumer<String> listener) {
        List<Consumer<String>> topicListeners = listeners.get(topic);
        if (topicListeners == null) {
            return;
        }
        topicListeners.remove(listener);
        if (topicListeners.isEmpty()) {
            listeners.remove(topic);
        }
    }

    private synchronized void ensureConnected() {
        if ((session != null && session.isConnected()) || connecting) {
            return;
        }
        connecting = true;
        stompClient.connectAsync(URL, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession connectedSession, StompHeaders connectedHeaders) {
                session = connectedSession;
                connecting = false;
                subscribedTopics.clear();
                for (String topic : listeners.keySet()) {
                    subscribeTopicIfNeeded(topic);
                }
            }

            @Override
            public void handleTransportError(StompSession currentSession, Throwable exception) {
                connecting = false;
                session = null;
                subscribedTopics.clear();
            }
        });
    }

    private void subscribeTopicIfNeeded(String topic) {
        StompSession currentSession = session;
        if (currentSession == null || !currentSession.isConnected() || !subscribedTopics.add(topic)) {
            return;
        }
        currentSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = String.valueOf(payload);
                List<Consumer<String>> topicListeners = listeners.get(topic);
                if (topicListeners == null || topicListeners.isEmpty()) {
                    return;
                }
                Platform.runLater(() -> {
                    for (Consumer<String> listener : topicListeners) {
                        listener.accept(message);
                    }
                });
            }
        });
    }
}
*/