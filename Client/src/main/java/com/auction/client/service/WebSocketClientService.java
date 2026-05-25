package com.auction.client.service;

import com.auction.client.controller.MainLayoutController;
import javafx.application.Platform;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.Map;

public class WebSocketClientService {
    private final ObjectMapper objectMapper = new JsonMapper();

    public void connect() {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new StringMessageConverter());

        String url = "ws://localhost:8080/ws";  //cổng kết nối tới springboot công khai

        stompClient.connect(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println("WebSocket: connected successfully to Server");

                Long userId = AppContext.getInstance().getUserId();

                String myPrivateChannel = "/topic/users" + userId + "/items";

                //đăng ký lắng nghe kênh thông báo sản phẩm
                session.subscribe(myPrivateChannel, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return String.class;  //đọc gói tin thô dạng chuỗi JSON
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        String jsonString = (String)  payload;
                        try{
                            //Chuyển JSON thành Map
                            Map<String, Object> msgMap = objectMapper.readValue(jsonString, Map.class);
                            String noticeText = (String) msgMap.get("message");

                            //ép chạy trên giao diện JavaFx
                            Platform.runLater(() -> {
                                MainLayoutController mainLayoutController = AppContext.getInstance().getMainLayoutController();
                                if(mainLayoutController != null){
                                    //bắn thẳng nội dung tin nhắn vào hàm cập nhật quả chuông
                                    mainLayoutController.addNewNotification(noticeText);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
            @Override
            public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                exception.printStackTrace();
            }
        });
    }
}
