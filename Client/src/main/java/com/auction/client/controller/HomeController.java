package com.auction.client.controller;


import com.auction.client.service.SceneService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.io.IOException;

public class HomeController {
    @FXML
    private Label welcomeText;

    @FXML
    public void switchToLogin(ActionEvent actionEvent) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/auction/client/fxml/signin/login.fxml"));
            javafx.scene.Node source = (javafx.scene.Node) actionEvent.getSource();
            source.getScene().setRoot(loginRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
