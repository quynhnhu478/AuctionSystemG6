package com.auction.client.controller.auction;

import com.auction.client.service.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class AutoBidPopupController {
    @FXML
    private ImageView imgProductDetails;
    @FXML
    private Label lblItemName;
    @FXML
    private Label lblDescription;
    @FXML
    private Label lblCategory;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblCurrentHighest;
    @FXML
    private Label lblBidIncrement;
    @FXML
    private Label lblTimeRemaining;
    @FXML
    private Label lblMinBidAlert;
    @FXML
    private TextField txtMaxBidLimit;
    @FXML
    private Label lblBalance;
    @FXML
    private Button btnActivateAutoBid;
    @FXML
    private HBox paneNotification;

    public void initFromItem(Long itemId, String name, String description, String category,
                             double price, double bidIncrement, LocalDateTime startingTime,
                             LocalDateTime endTime, String imageUrl) {
        lblItemName.setText(name);
        lblDescription.setText(description == null || description.isBlank() ? "-" : description);
        lblCategory.setText(category);
        lblCurrentHighest.setText(String.format("$%.2f", price));
        lblBidIncrement.setText(String.format("+$%.2f", bidIncrement));
        lblMinBidAlert.setText(String.format("Set Your Maximum Bid Limit (Min: $%.2f)", price + bidIncrement));
        txtMaxBidLimit.setPromptText(String.format("%.2f", price + bidIncrement));

        if (Session.getUser() != null) {
            lblBalance.setText(String.format("Your balance: $%.2f", Session.getUser().getBalance()));
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            String fullUrl = imageUrl.startsWith("http") ? imageUrl : "http://localhost:8080" + imageUrl;
            imgProductDetails.setImage(new Image(fullUrl, true));
        }

        updateStatus(startingTime, endTime);
    }

    @FXML
    private void handleActivateAutoBid() {
        try {
            double maxBid = Double.parseDouble(txtMaxBidLimit.getText().trim());
            if (maxBid <= 0) {
                showAlert(Alert.AlertType.WARNING, "Invalid limit", "Please enter a valid max bid limit.");
                return;
            }
            paneNotification.setVisible(true);
            paneNotification.setManaged(true);
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Invalid limit", "Please enter a valid max bid limit.");
        }
    }

    private void updateStatus(LocalDateTime startingTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (startingTime != null && now.isBefore(startingTime)) {
            lblStatus.setText("UPCOMING");
            lblTimeRemaining.setText("Not started");
            btnActivateAutoBid.setDisable(true);
            return;
        }
        if (endTime != null && now.isAfter(endTime)) {
            lblStatus.setText("CLOSED");
            lblTimeRemaining.setText("00h 00m 00s");
            btnActivateAutoBid.setDisable(true);
            return;
        }
        lblStatus.setText("OPEN");
        if (endTime != null) {
            long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            lblTimeRemaining.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
