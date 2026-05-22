package com.auction.client.controller.seller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class BecomeASellerController {

    @FXML
    private TextField sellerNameField;
    @FXML
    private TextField identityField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField addressField;
    @FXML
    private CheckBox agreeCheckBox;
    @FXML
    private Hyperlink termsLink;
    @FXML
    private Label formErrorLabel;
    @FXML
    private Button continueButton;

    @FXML
    private void initialize() {
        continueButton.setDisable(true);

        agreeCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            continueButton.setDisable(!newVal);
        });
    }

    @FXML
    private void handleTermsLink(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/terms-conditions-dialog.fxml"));
            Parent root = loader.load();
            TermsConditionsController termsController = loader.getController();
            termsController.setTargetCheckBox(agreeCheckBox);
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Terms & Conditions");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleContinue(ActionEvent event) {
        formErrorLabel.setText("");

        if (sellerNameField.getText().isBlank()
                || identityField.getText().isBlank()
                || phoneField.getText().isBlank()
                || emailField.getText().isBlank()
                || addressField.getText().isBlank()) {
            formErrorLabel.setText("Please complete all required fields.");
            return;
        }

        if (!agreeCheckBox.isSelected()) {
            formErrorLabel.setText("You must agree to the Terms & Conditions.");
            return;
        }

        openRegisterSellerDialog();
    }

    private void openRegisterSellerDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/register-seller-dialog.fxml"));
            Parent root = loader.load();
            RegisterSellerDialogController controller = loader.getController();
            
            Stage parentStage = (Stage) continueButton.getScene().getWindow();
            controller.setParentStage(parentStage);
            
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Verify Identity");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        Stage stage = (Stage) continueButton.getScene().getWindow();
        stage.close();
    }

}