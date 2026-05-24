package com.auction.client.controller.admin;

import com.auction.client.service.SceneService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class AdminDashboardController {

    @FXML
    public void switchToManageUserButton(ActionEvent event){
        SceneService.changeScene(event, "/com/auction/client/fxml/Admin/AdminManageUsers.fxml");

    }
    @FXML
    public void switchToManageAuctionsButton(ActionEvent event){
        SceneService.changeScene(event, "/com/auction/client/fxml/Admin/AdminDashboard.fxml");
    }
}
