package com.auction.client.controller.auction;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import tools.jackson.databind.JsonNode;
import com.auction.client.service.WebsocketConfigService;
import javafx.application.Platform;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import tools.jackson.databind.ObjectMapper;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class    ProductCardController  {
    // Initialized Logger for class diagnostics
    private static final Logger logger = Logger.getLogger(ProductCardController.class.getName());

    @FXML
    private VBox rootCard;
    @FXML
    private ImageView imgProduct;
    @FXML
    private Label lblItemName;
    @FXML
    private Label lblDescription;
    @FXML
    private Label lblCategory;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblPrice;
    @FXML
    private Label lblBidCount;
    @FXML
    private Label lblTimeRemaining;
    @FXML
    private Button btnPlaceBid;
    @FXML
    private Button btnAutoBid;

    private Timeline countdownTimeline;
    private Long itemId;
    private String itemName;
    private String itemDescription;
    private String itemCategory;
    private double itemPrice;
    private double bidIncrement;
    private LocalDateTime startingTime;
    private LocalDateTime endTime;
    private String imageUrl;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void onAuctionUpdated(double currentPrice, int bidCount) {
        Platform.runLater(() -> {
            // Gán trực tiếp giá trị nhận được từ popup con vào đây!
            lblPrice.setText(String.format("$%.2f", currentPrice));
            lblBidCount.setText(String.valueOf(bidCount));
        });
    }

    public void bindFromJson(JsonNode item) {
        itemId = item.path("id").asLong(0);
        itemName = item.path("name").asText("Unknown item");
        itemDescription = item.path("description").asText("");
        itemCategory = item.path("categories").asText("N/A");
        itemPrice = item.path("price").asDouble(0);
        bidIncrement = item.path("bidIncrement").asDouble(0);
        startingTime = parseDateTime(item.path("startingTime").asText(null));
        endTime = parseDateTime(item.path("endTime").asText(null));
        imageUrl = item.path("imageUrl").asText("");

        lblItemName.setText(itemName);
        lblDescription.setText(itemDescription.isBlank() ? "-" : itemDescription);
        lblCategory.setText(itemCategory);
        lblPrice.setText(String.format("$%.2f", itemPrice));
        lblBidCount.setText(String.valueOf(item.path("bidCount").asInt(0)));

        loadImage(imageUrl);
        startCountdown();
        subscribeBidCountUpdates();
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        openAuctionDetailsPopup(event);
    }

    @FXML
    public void handleAutoBid(ActionEvent event) {
        try {
            URL popupUrl = getClass().getResource("/com/auction/client/fxml/auction/AutoBidPopup.fxml");
            if (popupUrl == null) {
                throw new IllegalStateException("Cannot find AutoBidPopup.fxml");
            }

            FXMLLoader loader = new FXMLLoader(popupUrl);
            Parent root = loader.load();
            AutoBidPopupController controller = loader.getController();
            controller.setupCountdown(this.startingTime, this.endTime);
            controller.initFromItem(itemId, itemName, itemDescription, itemCategory, itemPrice, bidIncrement, startingTime, endTime, imageUrl);

            Stage stage = new Stage();
            stage.setTitle("Auto-Bid");
            stage.initOwner(((Node) event.getSource()).getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setOnCloseRequest(closeEvent -> {
                controller.shutdown();
                logger.info("[UI] Background auction room WebSocket connection disconnected upon closing Auto-Bid window.");
            });
            stage.showAndWait();
            stage.setOnHidden(e -> controller.shutdown());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occurred while initializing Auto-Bid window popup", e);
            showPopupError("Cannot open Auto-Bid", e);
        }
    }

    private void openAuctionDetailsPopup(ActionEvent event) {
        try {
            URL popupUrl = getClass().getResource("/com/auction/client/fxml/auction/AuctionDetailsPopup.fxml");
            if (popupUrl == null) {
                throw new IllegalStateException("Cannot find AuctionDetailsPopup.fxml");
            }

            FXMLLoader loader = new FXMLLoader(popupUrl);
            Parent root = loader.load();
            AuctionDetailsPopupController controller = loader.getController();
            controller.setupCountdown(this.startingTime, this.endTime);
            controller.setUpdateListener(this);
            controller.initFromItem(itemId, itemName, itemDescription, itemCategory, itemPrice, bidIncrement, startingTime, endTime, imageUrl);

            Stage stage = new Stage();
            stage.setTitle("Auction Details");
            stage.initOwner(((Node) event.getSource()).getScene().getWindow());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.showAndWait();
            stage.setOnHidden(e -> controller.stopTimeline());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occurred while initializing Auction Details window popup", e);
            showPopupError("Cannot open Auction Details", e);
        }
    }

    private void showPopupError(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(e.getMessage() == null ? e.toString() : e.getMessage());
        alert.showAndWait();
    }

    private void loadImage(String urlPath) {
        if (urlPath == null || urlPath.isBlank()) {
            return;
        }
        String fullUrl = urlPath.startsWith("http") ? urlPath : "http://localhost:8080" + urlPath;
        imgProduct.setImage(new Image(fullUrl, true));
    }

    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        if (startingTime == null || endTime == null) {
            lblTimeRemaining.setText("--");
            lblStatus.setText("UNKNOWN");
            return;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(startingTime)) {
                lblTimeRemaining.setText("Not started");
                lblStatus.setText("UPCOMING");
                lblStatus.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
                btnPlaceBid.setDisable(true);
                btnAutoBid.setDisable(true);
            } else if (now.isAfter(endTime)) {
                lblTimeRemaining.setText("00h 00m 00s");
                lblStatus.setText("CLOSED");
                lblStatus.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
                btnPlaceBid.setDisable(true);
                btnAutoBid.setDisable(true);
            } else {
                long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;
                lblTimeRemaining.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
                lblStatus.setText("OPEN");
                lblStatus.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
                btnPlaceBid.setDisable(false);
                btnAutoBid.setDisable(false);
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
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
    private void subscribeBidCountUpdates() {
        if (itemId == null || itemId <= 0) {
            return;
        }

        WebsocketConfigService.getInstance().subscribeAuctionRoom(itemId, this::refreshBidCount);
    }
    private void refreshBidCount() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/items"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        return;
                    }

                    try {
                        JsonNode root = mapper.readTree(response.body());
                        if (!root.isArray()) {
                            return;
                        }

                        for (JsonNode item : root) {
                            if (item.path("id").asLong(-1) == itemId) {
                                int bidCount = item.path("bidCount").asInt(0);
                                double currentPrice = item.path("price").asDouble(itemPrice);

                                Platform.runLater(() -> {
                                    lblBidCount.setText(String.valueOf(bidCount));
                                    lblPrice.setText(String.format("$%.2f", currentPrice));
                                });
                                return;
                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Cannot refresh bid count", e);
                    }
                });
    }
}