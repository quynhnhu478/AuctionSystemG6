package com.auction.client.controller.auction;

import com.auction.client.service.Session;
import com.auction.client.service.AuctionWebSocketService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class AuctionDetailsPopupController {
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
    private Label lblStartingPrice;
    @FXML
    private Label lblCurrentHighest;
    @FXML
    private Label lblTimeRemaining;
    @FXML
    private Label lblMinBidAlert;
    @FXML
    private TextField txtBidAmount;
    @FXML
    private Label lblBalance;
    @FXML
    private Button btnSubmitBid;
    @FXML
    private HBox paneNotification;
    @FXML
    private Label lblBidHistoryCount;
    @FXML
    private VBox vboxBidList;
    @FXML
    private Label lblNoBidsYet;

    private Long itemId;
    private double startingPrice;
    private double currentPrice;
    private double bidIncrement;
    private LocalDateTime startingTime;
    private LocalDateTime endTime;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private String auctionTopic;
    private java.util.function.Consumer<String> auctionUpdateListener;

    public void initFromItem(Long itemId, String name, String description, String category,
                             double price, double bidIncrement, LocalDateTime startingTime,
                             LocalDateTime endTime, String imageUrl) {
        this.itemId = itemId;
        this.startingPrice = price;
        this.currentPrice = price;
        this.bidIncrement = bidIncrement;
        this.startingTime = startingTime;
        this.endTime = endTime;

        lblItemName.setText(name);
        lblDescription.setText(description == null || description.isBlank() ? "-" : description);
        lblCategory.setText(category);
        lblStartingPrice.setText(String.format("$%.2f", startingPrice));
        updateCurrentPriceLabels();

        if (Session.getUser() != null) {
            lblBalance.setText(String.format("Your balance: $%.2f", Session.getUser().getBalance()));
        }

        if (imageUrl != null && !imageUrl.isBlank()) {
            String fullUrl = imageUrl.startsWith("http") ? imageUrl : "http://localhost:8080" + imageUrl;
            imgProductDetails.setImage(new Image(fullUrl, true));
        }

        updateStatus(startingTime, endTime);
        loadBidHistory();
        subscribeAuctionUpdates();
        imgProductDetails.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && auctionTopic != null && auctionUpdateListener != null) {
                AuctionWebSocketService.getInstance().unsubscribe(auctionTopic, auctionUpdateListener);
                auctionUpdateListener = null;
            }
        });
    }

    @FXML
    private void handleSubmitBid() {
        if (itemId == null || Session.getUser() == null) {
            showAlert(Alert.AlertType.WARNING, "Error", "Please login again.");
            return;
        }

        double minBid = currentPrice + bidIncrement;
        double amount;
        try {
            amount = Double.parseDouble(txtBidAmount.getText().trim());
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Invalid amount", "Please enter a valid bid amount.");
            return;
        }

        if (amount < minBid) {
            showAlert(Alert.AlertType.WARNING, "Bid too low", String.format("Minimum bid is $%.2f", minBid));
            return;
        }
        if (Session.getUser().getBalance() < amount) {
            showAlert(
                    Alert.AlertType.WARNING,
                    "Insufficient balance",
                    String.format("Your balance is $%.2f. You cannot bid $%.2f.", Session.getUser().getBalance(), amount)
            );
            return;
        }

        String json = String.format(
                "{\"auctionId\":%d,\"userId\":%d,\"bidAmount\":%.2f}",
                itemId, Session.getUser().getId(), amount
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/bids"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> handleBidResponse(response, amount)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Error", "Could not connect to server"));
                    return null;
                });
    }

    private void handleBidResponse(HttpResponse<String> response, double amount) {
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            applyAuctionUpdate(response.body());
            paneNotification.setVisible(true);
            paneNotification.setManaged(true);
            txtBidAmount.clear();
            loadBidHistory();
            return;
        }

        String message = "Failed to place bid. Code: " + response.statusCode();
        try {
            JsonNode root = mapper.readTree(response.body());
            if (root.has("message")) {
                message = root.path("message").asText(message);
            }
        } catch (Exception ignored) {
        }
        showAlert(Alert.AlertType.ERROR, "Bid failed", message);
    }

    private void loadBidHistory() {
        if (itemId == null) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/bids/item/" + itemId))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> renderBidHistory(response)))
                .exceptionally(ex -> null);
    }

    private void renderBidHistory(HttpResponse<String> response) {
        vboxBidList.getChildren().clear();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            lblNoBidsYet.setVisible(true);
            lblNoBidsYet.setManaged(true);
            vboxBidList.getChildren().add(lblNoBidsYet);
            lblBidHistoryCount.setText("📈 Bid History (0 bids)");
            return;
        }

        try {
            JsonNode root = mapper.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                lblNoBidsYet.setVisible(true);
                lblNoBidsYet.setManaged(true);
                vboxBidList.getChildren().add(lblNoBidsYet);
                lblBidHistoryCount.setText("📈 Bid History (0 bids)");
                return;
            }

            double highest = startingPrice;
            int count = 0;
            for (JsonNode bid : root) {
                count++;
                double bidAmount = bid.path("bidAmount").asDouble(0);
                if (bidAmount > highest) {
                    highest = bidAmount;
                }
                vboxBidList.getChildren().add(createBidRow(bid));
            }

            currentPrice = highest;
            updateCurrentPriceLabels();
            lblBidHistoryCount.setText("📈 Bid History (" + count + " bids)");
            lblNoBidsYet.setVisible(false);
            lblNoBidsYet.setManaged(false);
        } catch (Exception e) {
            lblNoBidsYet.setVisible(true);
            lblNoBidsYet.setManaged(true);
            vboxBidList.getChildren().add(lblNoBidsYet);
            lblBidHistoryCount.setText("📈 Bid History (0 bids)");
        }
    }

    private HBox createBidRow(JsonNode bid) {
        String bidder = bid.path("bidderName").asText("Unknown");
        double amount = bid.path("bidAmount").asDouble(0);
        String time = formatBidTime(bid.path("bidTime").asText(""));

        Label nameLabel = new Label(bidder);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");
        Label amountLabel = new Label(String.format("$%.2f", amount));
        amountLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #10B981;");
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, nameLabel, spacer, amountLabel, timeLabel);
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 6; -fx-padding: 8 12 8 12;");
        return row;
    }

    private String formatBidTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        try {
            LocalDateTime time = LocalDateTime.parse(raw.replace(" ", "T"));
            return time.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return raw;
        }
    }

    private void updateCurrentPriceLabels() {
        lblCurrentHighest.setText(String.format("$%.2f", currentPrice));
        lblMinBidAlert.setText(String.format("Place Your Bid (Min: $%.2f)", currentPrice + bidIncrement));
        txtBidAmount.setPromptText(String.format("%.2f", currentPrice + bidIncrement));
    }

    private void subscribeAuctionUpdates() {
        if (itemId == null || auctionUpdateListener != null) {
            return;
        }
        auctionTopic = "/topic/auction-" + itemId;
        auctionUpdateListener = payload -> {
            applyAuctionUpdate(payload);
            loadBidHistory();
        };
        AuctionWebSocketService.getInstance().subscribe(auctionTopic, auctionUpdateListener);
    }

    private void applyAuctionUpdate(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            if (root.has("currentPrice")) {
                currentPrice = root.path("currentPrice").asDouble(currentPrice);
            }
            if (root.has("bidderBalance") && !root.path("bidderBalance").isNull() && Session.getUser() != null) {
                double updatedBalance = root.path("bidderBalance").asDouble(Session.getUser().getBalance());
                Session.getUser().setBalance(updatedBalance);
                lblBalance.setText(String.format("Your balance: $%.2f", updatedBalance));
            }
            if (root.has("endTime") && !root.path("endTime").isNull()) {
                LocalDateTime updatedEndTime = parseDateTime(root.path("endTime").asText(""));
                if (updatedEndTime != null) {
                    endTime = updatedEndTime;
                }
            }
            if (root.has("bidCount")) {
                lblBidHistoryCount.setText("Bid History (" + root.path("bidCount").asInt() + " bids)");
            }
            updateCurrentPriceLabels();
            updateStatus(startingTime, endTime);
        } catch (Exception ignored) {
        }
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(raw.replace(" ", "T"));
            } catch (Exception e) {
                return null;
            }
        }
    }

    private void updateStatus(LocalDateTime startingTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (startingTime != null && now.isBefore(startingTime)) {
            lblStatus.setText("UPCOMING");
            lblTimeRemaining.setText("Not started");
            btnSubmitBid.setDisable(true);
            return;
        }
        if (endTime != null && now.isAfter(endTime)) {
            lblStatus.setText("CLOSED");
            lblTimeRemaining.setText("00h 00m 00s");
            btnSubmitBid.setDisable(true);
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
