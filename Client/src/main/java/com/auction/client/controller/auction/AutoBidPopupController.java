package com.auction.client.controller.auction;

import com.auction.client.service.AuctionUpdateListener;
import com.auction.client.service.Session;
import com.auction.client.service.WebsocketConfigService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javafx.util.Duration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class AutoBidPopupController {
    @FXML private ImageView imgProductDetails;
    @FXML private Label lblItemName;
    @FXML private Label lblDescription;
    @FXML private Label lblCategory;
    @FXML private Label lblStatus;
    @FXML private Label lblCurrentHighest;
    @FXML private Label lblBidIncrement;
    @FXML private Label lblTimeRemaining;
    @FXML private Label lblMinBidAlert;
    @FXML private TextField txtMaxBidLimit;
    @FXML private Label lblBalance;
    @FXML private Button btnActivateAutoBid;
    @FXML private HBox paneNotification;
    @FXML private Label lblBidHistoryCount;
    @FXML private VBox vboxBidList;
    @FXML private Label lblNoBidsYet;

    private Timeline countdownTimeline;
    private Long itemId;
    private double currentPrice;
    private double bidIncrement;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private int realtimeActionCount = 0;

    private AuctionUpdateListener updateListener;

    public void setUpdateListener(AuctionUpdateListener listener) {
        this.updateListener = listener;
    }
    private void initWebSocketListener(Long auctionId) {
        WebsocketConfigService.getInstance().subscribeAuctionRoom(auctionId, () -> {
            System.out.println("====== [AUTOBID SOCKET] Nhận tín hiệu REFRESH_SIGNAL! Tiến hành nạp lại dữ liệu...");
            // Gọi hàm kéo data mới từ Server về ngầm, không block luồng UI chính
            refreshAuctionDataData();
        });
    }

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
        setupCountdown(startingTime, endTime); // Kích hoạt bộ đếm thời gian chạy ngược

        // Kích hoạt lắng nghe WebSocket ngay khi nạp dữ liệu xong
        initWebSocketListener(itemId);
        refreshAuctionDataData();
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

            String url = String.format("http://localhost:8080/api/bids/auto-register?auctionId=%d&userId=%d&maxBid=%.2f",
                    itemId, Session.getUser().getId(), maxBid);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.noBody())
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
                if (root.has("currentPrice")) {
                    currentPrice = root.path("currentPrice").asDouble(currentPrice);
                    lblCurrentHighest.setText(String.format("$%.2f", currentPrice));
                    lblMinBidAlert.setText(String.format("Set Your Maximum Bid Limit (Min: $%.2f)", currentPrice + bidIncrement));
                }
            } catch (Exception ignored) {}

            paneNotification.setVisible(true);
            paneNotification.setManaged(true);
            txtMaxBidLimit.setDisable(true);
            btnActivateAutoBid.setDisable(true);

            appendRealtimeLog("Auto-bid successfully activated!");
            return;
        }
        btnActivateAutoBid.setDisable(false);
        txtMaxBidLimit.setDisable(false);
        String message = "Failed to activate auto-bid. Code: " + response.statusCode();
        try {
            JsonNode root = mapper.readTree(response.body());
            if (root.has("message")) {
                message = root.path("message").asText(message);
            }
        } catch (Exception ignored) {
            if (response.body() != null && !response.body().isBlank()) {
                message = response.body();
            }
        }
        showAlert(Alert.AlertType.ERROR, "Auto-bid failed", message);
    }

    private void refreshAuctionDataData() {
        if (itemId == null) return;
        String url = "http://localhost:8080/api/bids/history/" + itemId;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode root = mapper.readTree(response.body());

                            if (root.isArray() && !root.isEmpty()) {
                                JsonNode latestBid = root.get(0);
                                double bidAmount = latestBid.path("bidAmount").asDouble(currentPrice);
                                String bidderName = latestBid.path("bidderName").asText("Anonymous");
                                Long bidderId = latestBid.has("userId") ? latestBid.path("userId").asLong() : null;

                                // 1. Cập nhật biến giá trị logic ngầm trước
                                this.currentPrice = bidAmount;

                                // 2. Đẩy toàn bộ các lệnh thay đổi giao diện vào Platform.runLater duy nhất
                                Platform.runLater(() -> {
                                    // Đồng bộ nhãn dán tiền tệ
                                    lblCurrentHighest.setText(String.format("$%.2f", currentPrice));
                                    lblMinBidAlert.setText(String.format("Set Your Maximum Bid Limit (Min: $%.2f)", currentPrice + bidIncrement));
                                    txtMaxBidLimit.setPromptText(String.format("%.2f", currentPrice + bidIncrement));
                                    if (this.updateListener != null) {
                                        int totalBids = root.isArray() ? root.size() : 0; // Số lượng phần tử trong mảng lịch sử chính là số lượt bid
                                        this.updateListener.onAuctionUpdated(currentPrice, totalBids);
                                    }
                                    // Đồng bộ lại ví nếu chính mình vừa bid
                                    if (Session.getUser() != null && bidderId != null && bidderId.equals(Session.getUser().getId())) {
                                        lblBalance.setText(String.format("Your balance: $%.2f", Session.getUser().getBalance()));
                                    }

                                    // Tạo thông điệp ghi log sinh động
                                    String logMessage;
                                    if (Session.getUser() != null && bidderId != null && bidderId.equals(Session.getUser().getId())) {
                                        logMessage = String.format("Your robot automatically placed a bid of $%.2f", currentPrice);
                                    } else {
                                        logMessage = String.format("Competition! %s placed a new bid of $%.2f", bidderName, currentPrice);
                                    }

                                    // Kiểm tra khử trùng lặp tin nhắn Log trên UI
                                    boolean isDuplicate = false;
                                    if (!vboxBidList.getChildren().isEmpty() && vboxBidList.getChildren().get(0) instanceof Label) {
                                        Label latestLabel = (Label) vboxBidList.getChildren().get(0);
                                        if (latestLabel.getText().contains(String.format("$%.2f", currentPrice))) {
                                            isDuplicate = true;
                                        }
                                    }

                                    if (!isDuplicate) {
                                        appendRealtimeLog(logMessage);
                                    }
                                });
                            } else {
                                // Nếu mảng history rỗng, vẫn phải ép UI về giá ban đầu trong luồng an toàn
                                Platform.runLater(() -> {
                                    lblCurrentHighest.setText(String.format("$%.2f", currentPrice));
                                    lblMinBidAlert.setText(String.format("Set Your Maximum Bid Limit (Min: $%.2f)", currentPrice + bidIncrement));
                                    txtMaxBidLimit.setPromptText(String.format("%.2f", currentPrice + bidIncrement));
                                });
                            }
                        } catch (Exception e) {
                            System.err.println("Lỗi parse JSON tại AutoBid UI: " + e.getMessage());
                        }
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Lỗi mạng HTTP GET History: " + ex.getMessage());
                    return null;
                });
    }

    private void appendRealtimeLog(String message) {
        if (vboxBidList == null) return;

        if (lblNoBidsYet != null && lblNoBidsYet.isVisible()) {
            vboxBidList.getChildren().remove(lblNoBidsYet);
            lblNoBidsYet.setVisible(false);
            lblNoBidsYet.setManaged(false);
        }

        realtimeActionCount++;
        if (lblBidHistoryCount != null) {
            lblBidHistoryCount.setText("Real-time Log (" + realtimeActionCount + " actions)");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Label row = new Label("[" + timestamp + "] " + message);
        row.setMaxWidth(Double.MAX_VALUE);
        row.setWrapText(true);
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 6; -fx-padding: 8 12 8 12; -fx-text-fill: #475569; -fx-font-weight: bold;");

        vboxBidList.getChildren().add(0, row); // Chèn dòng log mới lên đầu bảng
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
        btnActivateAutoBid.setDisable(false);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setupCountdown(LocalDateTime startingTime, LocalDateTime endTime) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        if (startingTime == null || endTime == null) {
            lblTimeRemaining.setText("--");
            return;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(startingTime)) {
                lblTimeRemaining.setText("Not started");
                btnActivateAutoBid.setDisable(true);
            } else if (now.isAfter(endTime)) {
                lblTimeRemaining.setText("00h 00m 00s");
                lblTimeRemaining.setStyle("-fx-text-fill: red;");
                btnActivateAutoBid.setDisable(true);
                countdownTimeline.stop();
            } else {
                long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;
                lblTimeRemaining.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
                btnActivateAutoBid.setDisable(false);
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    public void shutdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        WebsocketConfigService.getInstance().unsubscribeAuctionRoom();
    }
}