package com.auction.client.controller.seller;

import com.auction.client.controller.MainLayoutController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Stage;

public class ApplicationSubmittedDialogController {

    private Stage registerSellerStage;
    private Stage becomeSellerStage;
    private MainLayoutController mainLayoutController;
    private boolean hasclosed = false;

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