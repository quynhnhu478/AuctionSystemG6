package com.auction.client.service;
//Để lấy id người dùng cho item
public class AppContext {
    private static AppContext instance;
    private String token;
    private Long userId;

    private AppContext() {}  //Singleton Pattern

    public static AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}

