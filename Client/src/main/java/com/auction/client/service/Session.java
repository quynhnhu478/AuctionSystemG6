package com.auction.client.service;



public class Session {
    private static UserResponse currentUser;
    public static void setUser(UserResponse user) {
        currentUser = user;
    }
    public static UserResponse getUser(){
        return currentUser;
    }
}
