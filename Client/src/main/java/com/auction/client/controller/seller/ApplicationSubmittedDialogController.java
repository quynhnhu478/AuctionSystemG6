package com.auction.client.controller.seller;

import com.auction.client.controller.MainLayoutController;
import com.auction.client.service.AppEventBus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class ApplicationSubmittedDialogController {

    private Stage registerSellerStage;
    private Stage becomeSellerStage;
    private MainLayoutController mainLayoutController;
    private boolean hasclosed = false;

    @FXML
    public void initialize(){

    }

    public boolean getHasclosed() {
        return hasclosed;
    }
    @FXML
    private void handleClose(ActionEvent event) {
        // Set trạng thái seller application đã được submit
        MainLayoutController.setSellerApplicationSubmitted(true);
        hasclosed = true;
        // Đóng các Stage hiện tại
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();

    }
}