package com.auction.client.controller;

import com.auction.client.service.Session;

import com.auction.common.payload.UserResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfileController {
    @FXML
    private Label username;
    @FXML
    private Label useremail;

    public void initialize(){
        UserResponse user = Session.getUser();

        if (user != null) {
            username.setText("Hello " + user.getName());
            useremail.setText("Role: " + user.getEmail());
        }
    }
}
