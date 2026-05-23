package com.auction.client.service;

import com.auction.client.controller.MainLayoutController;
import com.auction.client.controller.seller.ItemContainerController;

//Để lấy id người dùng cho item
public class AppContext {
    private static AppContext instance;
    private String token;
    private Long userId;
    private MainLayoutController mainLayoutController;
    private ItemContainerController itemContainerController;

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

    public void  setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    public MainLayoutController getMainLayoutController() {
        return mainLayoutController;
    }

    public void setItemContainerController(ItemContainerController itemContainerController) {
        this.itemContainerController = itemContainerController;
    }

    public ItemContainerController getItemContainerController() {
        return itemContainerController;
    }
}

