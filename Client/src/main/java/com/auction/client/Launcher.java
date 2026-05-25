package com.auction.client;

import com.auction.client.app.HelloApplication;
import com.auction.client.service.WebSocketClientService;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // Trong hàm start(Stage primaryStage) hoặc chỗ nào app vừa bật lên thành công:
        WebSocketClientService webSocketService = new WebSocketClientService();
        webSocketService.connect(); // 🎯 Kích hoạt chế độ lắng nghe ngầm real-time
        Application.launch(HelloApplication.class, args);
    }
}
