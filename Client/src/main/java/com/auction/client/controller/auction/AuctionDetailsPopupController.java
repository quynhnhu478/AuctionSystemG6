package com.auction.client.controller.auction;

import com.auction.client.service.AppEventBus;
import com.auction.client.service.Session;
import com.auction.common.payload.ItemResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuctionDetailsPopupController {
    @FXML private ImageView imgProductDetails;
    @FXML private Label lblItemName;
    @FXML private Label lblDescription;
    @FXML private Label lblCategory;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentHighest;
    @FXML private Label lblMinBidAlert;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnSubmitBid;

    private static final String SERVER_IMAGE_URL = "http://localhost:8080/uploads/items/";
    private Long auctionId;
    private Long userId;
    private ItemResponse itemData;

    @FXML
    public void initialize() {
        btnSubmitBid.setOnAction(event -> handlePlaceBid());

        AppEventBus.on("AUCTION_PRICE_UPDATED", (Object data) -> {
            if (data == null) return;
            String jsonPayload = (String) data;

            Platform.runLater(() -> {
                try {
                    tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.json.JsonMapper()
                            .builder()
                            .addModule(new tools.jackson.datatype.jsr310.JavaTimeModule())
                            .build();
                    tools.jackson.databind.JsonNode rootNode = mapper.readTree(jsonPayload);
                    long id = rootNode.get("id").asLong();
                    if (auctionId != null && id == auctionId) {
                        double currentPrice = rootNode.get("currentPrice").asDouble();
                        lblCurrentHighest.setText(String.format("$%,.2f", currentPrice));
                        if (itemData != null) {
                            lblMinBidAlert.setText(String.format("Place Your Bid (Min: $%,.2f)", currentPrice + itemData.getBidIncrement()));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing auction update in popup: " + e.getMessage());
                }
            });
        });
    }

    public void setAuctionData(ItemResponse itemData) {
        if (itemData == null) return;

        this.itemData = itemData;
        this.auctionId = itemData.getId();
        lblItemName.setText(itemData.getName());
        lblDescription.setText(itemData.getDescription());
        lblCategory.setText(itemData.getCategories() == null ? "Category" : itemData.getCategories().toString());
        lblStartingPrice.setText(String.format("$%,.2f", itemData.getPrice()));
        lblCurrentHighest.setText(String.format("$%,.2f", itemData.getPrice()));
        lblMinBidAlert.setText(String.format("Place Your Bid (Min: $%,.2f)", itemData.getPrice() + itemData.getBidIncrement()));

        if (itemData.getImageUrl() != null && !itemData.getImageUrl().isEmpty()) {
            imgProductDetails.setImage(new Image(SERVER_IMAGE_URL + itemData.getImageUrl(), true));
        }

        if (Session.getUser() != null) {
            this.userId = Session.getUser().getId();
        } else {
            this.userId = 1L;
        }
    }

    private void handlePlaceBid() {
        String amountText = txtBidAmount.getText().trim();
        if (amountText.isEmpty()) {
            showNotification("Error", "Please enter a bid amount.");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(amountText);
            if (userId == null || auctionId == null) return;

            String url = String.format("http://localhost:8080/api/auction/bid?userId=%d&auctionId=%d&amount=%f",
                    userId, auctionId, bidAmount);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpClient.newHttpClient()
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            showNotification("Success", "Your bid was placed successfully.");
                            txtBidAmount.clear();
                        } else {
                            showNotification("Bid failed", response.body());
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showNotification("Connection error", "Unable to connect to the server."));
                        return null;
                    });
        } catch (NumberFormatException e) {
            showNotification("Input error", "Bid amount must be a valid number.");
        }
    }

    private void showNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
