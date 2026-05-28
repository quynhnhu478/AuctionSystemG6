/*package com.auction.client.controller.auction;

import com.auction.client.service.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MyBidsController {
    @FXML
    private VBox bidsList;
    @FXML
    private Label emptyLabel;
    @FXML
    private Button refreshButton;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    private void initialize() {
        loadMyBids();
    }

    @FXML
    private void loadMyBids() {
        if (Session.getUser() == null || Session.getUser().getId() == null) {
            showEmpty("Session expired. Please login again.");
            return;
        }
        refreshButton.setDisable(true);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/bids/user/" + Session.getUser().getId()))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> render(response)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        refreshButton.setDisable(false);
                        showEmpty("Could not connect to server.");
                    });
                    return null;
                });
    }

    private void render(HttpResponse<String> response) {
        refreshButton.setDisable(false);
        bidsList.getChildren().clear();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            showEmpty("Failed to load your bids. Code: " + response.statusCode());
            return;
        }
        try {
            JsonNode root = mapper.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                showEmpty("You have not placed any bids yet.");
                return;
            }
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
            for (JsonNode bid : root) {
                bidsList.getChildren().add(createBidRow(bid));
            }
        } catch (Exception e) {
            showEmpty("Failed to parse bid data.");
        }
    }

    private HBox createBidRow(JsonNode bid) {
        String itemName = bid.path("itemName").asText("Auction #" + bid.path("auctionId").asText(""));
        double bidAmount = bid.path("bidAmount").asDouble(0);
        double currentPrice = bid.path("currentPrice").asDouble(bidAmount);
        boolean winning = bid.path("winningBid").asBoolean(false);

        VBox textBox = new VBox(4);
        Label title = new Label(itemName);
        title.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label meta = new Label("Bid time: " + formatTime(bid.path("bidTime").asText("")));
        meta.setStyle("-fx-font-size: 12; -fx-text-fill: #64748B;");
        textBox.getChildren().addAll(title, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox priceBox = new VBox(4);
        priceBox.setStyle("-fx-alignment: center-right;");
        Label amount = new Label(String.format("Your bid: $%.2f", bidAmount));
        amount.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #10B981;");
        Label status = new Label(winning ? "Winning" : String.format("Current: $%.2f", currentPrice));
        status.setStyle(winning
                ? "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold;"
                : "-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold;");
        priceBox.getChildren().addAll(amount, status);

        HBox row = new HBox(12, textBox, spacer, priceBox);
        row.setStyle("-fx-background-color: white; -fx-border-color: #d9c9be; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12 14 12 14;");
        return row;
    }

    private String formatTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            return LocalDateTime.parse(raw.replace(" ", "T")).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return raw;
        }
    }

    private void showEmpty(String message) {
        bidsList.getChildren().clear();
        emptyLabel.setText(message);
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
    }
}
*/