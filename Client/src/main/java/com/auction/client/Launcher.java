package com.auction.client;

import com.auction.client.app.HelloApplication;
import com.auction.client.service.WebSocketClientService;
import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        Application.launch(HelloApplication.class, args);
    }
}
