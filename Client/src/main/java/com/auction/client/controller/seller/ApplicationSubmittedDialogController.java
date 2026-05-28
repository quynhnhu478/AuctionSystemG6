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
import java.util.logging.Logger;

public class ApplicationSubmittedDialogController {
    private static final Logger logger = Logger.getLogger(ApplicationSubmittedDialogController.class.getName());

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
        // Set seller application status to submitted
        MainLayoutController.setSellerApplicationSubmitted(true);
        hasclosed = true;

        // Close the current window stage
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();

        logger.info("[UI] Seller application dialog window successfully closed and state saved.");
    }
}