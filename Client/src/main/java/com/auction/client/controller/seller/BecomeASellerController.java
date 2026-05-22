package com.auction.client.controller.seller;

import com.auction.common.payload.SellerRegistrationRequest;
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

    private SellerRegistrationRequest request;


    // Hàm để màn hình cha truyền đối tượng vào
    public void setRegistrationRequest(SellerRegistrationRequest request) {
        this.request = request;
    }

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
        String name = sellerNameField.getText();
        String identity = identityField.getText();
        String phone = phoneField.getText();
        String email = emailField.getText();
        String address = addressField.getText();

        if (name.isBlank()
                || identity.isBlank()
                || phone.isBlank()
                || email.isBlank()
                || address.isBlank()) {
            formErrorLabel.setText("Please complete all required fields.");
            return;
        }

        if (!agreeCheckBox.isSelected()) {
            formErrorLabel.setText("You must agree to the Terms & Conditions.");
            return;
        }
        request.setName(name);
        request.setAddress(address);
        request.setEmail(email);
        request.setPhoneNumber(phone);
        request.setIdentityNumber(identity);

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