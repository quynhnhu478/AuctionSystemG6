package com.auction.client.controller.seller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

public class TermsConditionsController {

    private CheckBox targetCheckBox;

    public void setTargetCheckBox(CheckBox targetCheckBox) {
        this.targetCheckBox = targetCheckBox;
    }

    @FXML
    private void handleDecline(ActionEvent event) {
        if (targetCheckBox != null) {
            targetCheckBox.setSelected(false);
        }
        closeDialog(event);
    }

    @FXML
    private void handleAccept(ActionEvent event) {
        if (targetCheckBox != null) {
            targetCheckBox.setSelected(true);
        }
        closeDialog(event);
    }

    private void closeDialog(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}