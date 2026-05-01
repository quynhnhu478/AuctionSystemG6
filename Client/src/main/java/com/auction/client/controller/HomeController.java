package com.auction.client.controller;

import com.auction.client.service.SceneService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HomeController {
    @FXML
    private Label welcomeText;

    @FXML
    public void switchToRegister(ActionEvent event){
        SceneService.changeScene(event, "/com/auction/client/fxml/register.fxml");
    }

}
