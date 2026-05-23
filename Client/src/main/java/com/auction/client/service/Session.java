package com.auction.client.service;


import com.auction.common.payload.UserResponse;

public class Session {
    private static UserResponse currentUser;
    public static void setUser(UserResponse user) {
        currentUser = user;
    }
    public static UserResponse getUser(){
        return currentUser;
    }
}
