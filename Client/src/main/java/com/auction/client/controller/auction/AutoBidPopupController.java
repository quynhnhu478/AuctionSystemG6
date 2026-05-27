package com.auction.client.controller.auction;

import com.auction.client.service.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    @FXML
    private Label lblBidHistoryCount;
    @FXML
    private VBox vboxBidList;
    @FXML
    private Label lblNoBidsYet;

    private Long itemId;
    private double currentPrice;
    private double bidIncrement;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private StompSession stompSession;
    private int realtimeActionCount = 0;
    private java.util.function.Consumer<Object> balanceUpdateListener;

    public void initFromItem(Long itemId, String name, String description, String category,
                             double price, double bidIncrement, LocalDateTime startingTime,
                             LocalDateTime endTime, String imageUrl) {
        this.itemId = itemId;
        this.currentPrice = price;
        this.bidIncrement = bidIncrement;
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
        subscribeAuctionUpdates();

        balanceUpdateListener = balance -> {
            Platform.runLater(() -> {
                lblBalance.setText(String.format("Your balance: $%.2f", (Double) balance));
            });
        };
        com.auction.client.service.AppEventBus.on("BALANCE_UPDATED", balanceUpdateListener);
    }

    public void cleanup() {
        if (balanceUpdateListener != null) {
            com.auction.client.service.AppEventBus.off("BALANCE_UPDATED", balanceUpdateListener);
        }
        if (stompSession != null && stompSession.isConnected()) {
            try {
                stompSession.disconnect();
            } catch (Exception e) {
                System.err.println("Error disconnecting STOMP session: " + e.getMessage());
            }
            stompSession = null;
        }
    }

    @FXML
    private void handleActivateAutoBid() {
        if (itemId == null || Session.getUser() == null) {
            showAlert(Alert.AlertType.WARNING, "Error", "Please login again.");
            return;
        }
        try {
            double maxBid = Double.parseDouble(txtMaxBidLimit.getText().trim());
            double minBid = currentPrice + bidIncrement;
            if (maxBid < minBid) {
                showAlert(Alert.AlertType.WARNING, "Invalid limit", String.format("Minimum auto-bid limit is $%.2f", minBid));
                return;
            }

            String json = String.format(
                    "{\"auctionId\":%d,\"userId\":%d,\"maxBid\":%.2f,\"increment\":%.2f}",
                    itemId, Session.getUser().getId(), maxBid, bidIncrement
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/api/bids/auto"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> handleAutoBidResponse(response)))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Connection error", "Could not connect to server"));
                        return null;
                    });
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Invalid limit", "Please enter a valid max bid limit.");
        }
    }

    private void handleAutoBidResponse(HttpResponse<String> response) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            try {
                JsonNode root = mapper.readTree(response.body());
                currentPrice = root.path("currentPrice").asDouble(currentPrice);
                lblCurrentHighest.setText(String.format("$%.2f", currentPrice));
                lblMinBidAlert.setText(String.format("Set Your Maximum Bid Limit (Min: $%.2f)", currentPrice + bidIncrement));

                if (root.has("bidderBalance") && !root.path("bidderBalance").isNull() && Session.getUser() != null) {
                    double updatedBalance = root.path("bidderBalance").asDouble(Session.getUser().getBalance());
                    Session.getUser().setBalance(updatedBalance);
                    lblBalance.setText(String.format("Your balance: $%.2f", updatedBalance));
                }
            } catch (Exception ignored) {
            }
            paneNotification.setVisible(true);
            paneNotification.setManaged(true);
            btnActivateAutoBid.setDisable(true);
            appendRealtimeLog("Auto-bid activated");
            return;
        }

        String message = "Failed to activate auto-bid. Code: " + response.statusCode();
        try {
            JsonNode root = mapper.readTree(response.body());
            if (root.has("message")) {
                message = root.path("message").asText(message);
            }
        } catch (Exception ignored) {
        }
        showAlert(Alert.AlertType.ERROR, "Auto-bid failed", message);
    }

    private void subscribeAuctionUpdates() {
        if (itemId == null || stompSession != null) {
            return;
        }
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new StringMessageConverter());
        stompClient.connectAsync("ws://localhost:8080/ws-auction", new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                stompSession = session;
                session.subscribe("/topic/auction-" + itemId, new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        Platform.runLater(() -> applyAuctionUpdate(String.valueOf(payload)));
                    }
                });
            }
        });
    }

    private void applyAuctionUpdate(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            if (root.has("currentPrice")) {
                currentPrice = root.path("currentPrice").asDouble(currentPrice);
                lblCurrentHighest.setText(String.format("$%.2f", currentPrice));
                lblMinBidAlert.setText(String.format("Set Your Maximum Bid Limit (Min: $%.2f)", currentPrice + bidIncrement));
            }
            String message = root.path("message").asText("Auction updated");
            String winner = root.path("winnerName").asText("");
            if (!winner.isBlank()) {
                message += " - highest bidder: " + winner;
            }
            appendRealtimeLog(message);
        } catch (Exception ignored) {
        }
    }

    private void appendRealtimeLog(String message) {
        if (vboxBidList == null) {
            return;
        }
        if (lblNoBidsYet != null) {
            vboxBidList.getChildren().remove(lblNoBidsYet);
            lblNoBidsYet.setVisible(false);
            lblNoBidsYet.setManaged(false);
        }
        realtimeActionCount++;
        if (lblBidHistoryCount != null) {
            lblBidHistoryCount.setText("Real-time Log (" + realtimeActionCount + " actions)");
        }
        Label row = new Label(message);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setWrapText(true);
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-text-fill: #475569;");
        vboxBidList.getChildren().add(0, row);
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
