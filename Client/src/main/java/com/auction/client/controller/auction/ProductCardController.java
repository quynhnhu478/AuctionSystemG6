package com.auction.client.controller.auction;

import com.auction.client.service.AuctionUpdateListener;
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
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.auction.client.service.Session;
public class ProductCardController implements AuctionUpdateListener {
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
    private long serverTimeOffsetSeconds = 0;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private Long sellerId;
    private Long auctionId;
    private java.util.List<String> imageUrls = new java.util.ArrayList<>();
    private String auctionStatus;
    private Consumer<String> auctionUpdateListener;

    @Override
    public void onAuctionUpdated(double currentPrice, int bidCount) {
        Platform.runLater(() -> {
            // Gán trực tiếp giá trị nhận được từ popup con vào đây!
            lblPrice.setText(String.format("$%.2f", currentPrice));
            lblBidCount.setText(String.valueOf(bidCount));
        });
    }
    private LocalDateTime nowFromServerClock() {
        return LocalDateTime.now().plusSeconds(serverTimeOffsetSeconds);
    }
    public void bindFromJson(JsonNode item) {
        itemId = item.path("id").asLong(0);
        auctionId = item.path("auctionId").isMissingNode() || item.path("auctionId").isNull()
                ? itemId
                : item.path("auctionId").asLong();
        auctionStatus = item.path("auctionStatus").asText("");
        itemName = item.path("name").asText("Unknown item");
        itemDescription = item.path("description").asText("");
        itemCategory = item.path("categories").asText("N/A");
        itemPrice = item.path("price").asDouble(0);
        bidIncrement = item.path("bidIncrement").asDouble(0);
        startingTime = parseDateTime(item.path("startingTime").asText(null));
        endTime = parseDateTime(item.path("endTime").asText(null));
        if (item.has("serverTime") && !item.get("serverTime").isNull()) {
            LocalDateTime serverTime = parseDateTime(item.get("serverTime").asText());
            if (serverTime != null) {
                serverTimeOffsetSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), serverTime);
            }
        }
        imageUrl = item.path("imageUrl").asText("");
        imageUrls.clear();

        if (item.has("imageUrls") && item.get("imageUrls").isArray()) {
            for (JsonNode img : item.get("imageUrls")) {
                imageUrls.add(img.asText());
            }
        }

        if (imageUrls.isEmpty() && imageUrl != null && !imageUrl.isBlank()) {
            imageUrls.add(imageUrl);
        }

        lblItemName.setText(itemName);

        lblItemName.setText(itemName);
        lblDescription.setText(itemDescription.isBlank() ? "-" : itemDescription);
        lblCategory.setText(itemCategory);
        lblPrice.setText(String.format("$%.2f", itemPrice));
        lblBidCount.setText(String.valueOf(item.path("bidCount").asInt(0)));

        loadImage(imageUrls.isEmpty() ? imageUrl : imageUrls.get(0));
        startCountdown();
        subscribeBidCountUpdates();
        sellerId = item.path("sellerId").isMissingNode() || item.path("sellerId").isNull()
                ? null
                : item.path("sellerId").asLong();

    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        if (Session.getUser() != null
                && sellerId != null
                && Session.getUser().getId().equals(sellerId)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Bid Placement Failed");
            alert.setHeaderText(null);
            alert.setContentText("Seller cannot bid their Items!");
            alert.showAndWait();
            return;
        }

        openAuctionDetailsPopup(event);
    }

    @FXML
    public void handleAutoBid(ActionEvent event) {
        if (Session.getUser() != null
                && sellerId != null
                && Session.getUser().getId().equals(sellerId)) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Auto-bid failed");
            alert.setHeaderText(null);
            alert.setContentText("Seller cannot bid their Items!");
            alert.showAndWait();
            return;
        }
        try {
            URL popupUrl = getClass().getResource("/com/auction/client/fxml/auction/AutoBidPopup.fxml");
            if (popupUrl == null) {
                throw new IllegalStateException("Cannot find AutoBidPopup.fxml");
            }

            FXMLLoader loader = new FXMLLoader(popupUrl);
            Parent root = loader.load();
            AutoBidPopupController controller = loader.getController();
            controller.setupCountdown(this.startingTime, this.endTime);
            controller.initFromItem(
                    auctionId,
                    itemName,
                    itemDescription,
                    itemCategory,
                    itemPrice,
                    bidIncrement,
                    startingTime,
                    endTime,
                    imageUrl
            );
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
            controller.setServerTimeOffsetSeconds(this.serverTimeOffsetSeconds);
            controller.initFromItem(
                    auctionId,
                    itemName,
                    itemDescription,
                    itemCategory,
                    itemPrice,
                    bidIncrement,
                    startingTime,
                    endTime,
                    imageUrl,
                    imageUrls
            );
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
        String fullUrl = normalizeImageUrl(urlPath);
        if (fullUrl.isBlank()) {
            return;
        }
        imgProduct.setImage(new Image(fullUrl, true));
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }
        if (imageUrl.startsWith("http")) {
            return imageUrl;
        }
        if (imageUrl.startsWith("/")) {
            return "http://localhost:8080" + imageUrl;
        }
        return "http://localhost:8080/uploads/items/" + imageUrl;
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

        updateCountdownStatus();
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateCountdownStatus()));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdownStatus() {
        if ("FINISHED".equalsIgnoreCase(auctionStatus)) {
            lblTimeRemaining.setText("00h 00m 00s");
            lblStatus.setText("FINISHED");
            // Finished: Chữ vàng đậm, Nền vàng nhạt, Viền vàng
            lblStatus.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #F57C00; -fx-border-color: #FFE082; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
            btnPlaceBid.setDisable(true);
            btnAutoBid.setDisable(true);
            return;
        }

        if ("PAID".equalsIgnoreCase(auctionStatus)) {
            lblTimeRemaining.setText("00h 00m 00s");
            lblStatus.setText("PAID");
            // Paid: Chữ tím đậm, Nền tím nhạt, Viền tím
            lblStatus.setStyle("-fx-background-color: #F3E5F5; -fx-text-fill: #4A148C; -fx-border-color: #E1BEE7; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
            btnPlaceBid.setDisable(true);
            btnAutoBid.setDisable(true);
            return;
        }

        if ("CANCELED".equalsIgnoreCase(auctionStatus)) {
            lblTimeRemaining.setText("00h 00m 00s");
            lblStatus.setText("CANCELED");
            // Cancel: Chữ xanh lam đậm (#0D47A1), Nền xanh lam nhạt (#E3F2FD), Viền xanh lam (#BBDEFB)
            lblStatus.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #0D47A1; -fx-border-color: #BBDEFB; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
            btnPlaceBid.setDisable(true);
            btnAutoBid.setDisable(true);
            return;
        }
        LocalDateTime now = nowFromServerClock();
        if (now.isBefore(startingTime)) {
            lblTimeRemaining.setText("Not started");
            lblStatus.setText("OPEN");
            lblStatus.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
            btnPlaceBid.setDisable(true);
            btnAutoBid.setDisable(true);
        } else if (now.isAfter(endTime)) {
            lblTimeRemaining.setText("00h 00m 00s");
            lblStatus.setText("CLOSED");
            lblStatus.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-border-color: #FFCC80; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
            btnPlaceBid.setDisable(true);
            btnAutoBid.setDisable(true);
        } else {
            long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            lblTimeRemaining.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
            lblStatus.setText("RUNNING".equalsIgnoreCase(auctionStatus) ? "RUNNING" : "OPEN");
            lblStatus.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
            btnPlaceBid.setDisable(false);
            btnAutoBid.setDisable(false);
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
    private void subscribeBidCountUpdates() {
        if (auctionId == null || auctionId <= 0) {
            return;
        }

        if (auctionUpdateListener != null) {
            WebsocketConfigService.getInstance().unsubscribeAuctionRoom(auctionId, auctionUpdateListener);
        }
        auctionUpdateListener = this::applyAuctionUpdateFromSocket;
        WebsocketConfigService.getInstance().subscribeAuctionRoom(auctionId, auctionUpdateListener);
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
                            if (item.path("auctionId").asLong(-1) == auctionId) {
                                int bidCount = item.path("bidCount").asInt(0);
                                double currentPrice = item.path("price").asDouble(itemPrice);
                                LocalDateTime newEndTime = parseDateTime(item.path("endTime").asText(null));
                                LocalDateTime newServerTime = parseDateTime(item.path("serverTime").asText(null));

                                String rawEndTime = item.path("endTime").asText(null);
                                LocalDateTime serverEndTime = parseDateTime(rawEndTime);
                                if (serverEndTime != null) {
                                    endTime = serverEndTime;
                                }

                                Platform.runLater(() -> {
                                    if (newEndTime != null) {
                                        endTime = newEndTime;
                                    }
                                    if (newServerTime != null) {
                                        serverTimeOffsetSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), newServerTime);
                                    }

                                    lblBidCount.setText(String.valueOf(bidCount));
                                    lblPrice.setText(String.format("$%.2f", currentPrice));

                                    lblBidCount.setText(String.valueOf(bidCount));
                                    lblPrice.setText(String.format("$%.2f", currentPrice));

                                    // ==================== KHÓA NÚT NGAY TẠI ĐÂY ====================
                                    // Kiểm tra xem tại thời điểm nhận tín hiệu, phiên đã lọt vào trạng thái kết thúc chưa
                                    LocalDateTime now = nowFromServerClock();
                                    if (endTime != null && now.isAfter(endTime)) {
                                        lblTimeRemaining.setText("00h 00m 00s");
                                        lblStatus.setText("CLOSED");
                                        lblStatus.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-border-color: #FFCC80; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");

                                        // Khóa cứng 2 nút ngay ngoài màn hình danh sách!
                                        btnPlaceBid.setDisable(true);
                                        btnAutoBid.setDisable(true);

                                        // Dừng bộ Timeline đếm ngược chạy ngầm của Card này lại
                                        if (countdownTimeline != null) {
                                            countdownTimeline.stop();
                                        }

                                        triggerServerToEndAuction(auctionId);
                                    }
                                    else {
                                        // Luồng đang diễn ra bình thường (OPEN)
                                        long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
                                        long hours = totalSeconds / 3600;
                                        long minutes = (totalSeconds % 3600) / 60;
                                        long seconds = totalSeconds % 60;

                                        lblTimeRemaining.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
                                        lblStatus.setText("OPEN");
                                        btnPlaceBid.setDisable(false);
                                        btnAutoBid.setDisable(false);

                                    }
                                });
                                countdownTimeline.setCycleCount(Animation.INDEFINITE);
                                countdownTimeline.play();
                                return;
                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Cannot refresh bid count", e);
                    }
                });
    }
    public void applySocketUpdate(JsonNode node) {
        JsonNode roomNode = node.has("roomUpdate") ? node.get("roomUpdate") : node;

        if (roomNode.has("auctionStatus") && !roomNode.get("auctionStatus").isNull()) {
            auctionStatus = roomNode.get("auctionStatus").asText();
        }

        if (roomNode.has("serverTime") && !roomNode.get("serverTime").isNull()) {
            LocalDateTime serverTime = parseDateTime(roomNode.get("serverTime").asText());
            if (serverTime != null) {
                serverTimeOffsetSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), serverTime);
            }
        }

        if (roomNode.has("currentPrice")) {
            itemPrice = roomNode.path("currentPrice").asDouble(itemPrice);
            lblPrice.setText(String.format("$%.2f", itemPrice));
        }

        if (roomNode.has("bidCount")) {
            lblBidCount.setText(String.valueOf(roomNode.path("bidCount").asInt()));
        }

        if (roomNode.has("endTime") && !roomNode.get("endTime").isNull()) {
            LocalDateTime updatedEndTime = parseDateTime(roomNode.get("endTime").asText());
            if (updatedEndTime != null) {
                endTime = updatedEndTime;
            }
        }

        updateCountdownStatus();
    }
    private void applyAuctionUpdateFromSocket(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            applySocketUpdate(root);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot apply auction socket update", e);
        }
    }

    public void dispose() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
        if (auctionId != null && auctionUpdateListener != null) {
            WebsocketConfigService.getInstance().unsubscribeAuctionRoom(auctionId, auctionUpdateListener);
            auctionUpdateListener = null;
        }
    }

    public void setServerTimeOffsetSeconds(long serverTimeOffsetSeconds) {
        this.serverTimeOffsetSeconds = serverTimeOffsetSeconds;
    }
    private void triggerServerToEndAuction(Long auctionId) {
        // Gọi đến API endAuctionManual mà tụi mình đã xây dựng ở Server
        String url = "http://localhost:8080/api/auctions/end-manual?auctionId=" + auctionId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        System.out.println("====== [CLIENT] Đã chốt hạ phiên " + auctionId + " thành công trên Database!");
                        // Sau khi Server chốt xong, không chạy lại hàm nạp đè data gốc nữa để giữ nguyên giá cao nhất hiển thị
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Lỗi đồng bộ kết thúc: " + ex.getMessage());
                    return null;
                });
    }

}
