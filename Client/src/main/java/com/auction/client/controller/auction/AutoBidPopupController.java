package com.auction.client.controller.auction;



import com.auction.client.service.Session;
import com.auction.client.service.WebsocketConfigService;

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
import java.lang.reflect.Type;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
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

    private Long itemId;
    private double currentPrice;
    private double bidIncrement;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private int realtimeActionCount = 0;

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

        // Tận dụng WebsocketConfigService Singleton để lắng nghe phòng đấu giá
        initWebSocketListener();
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

            // 1. CHỈNH SỬA: Chuyển sang dạng RequestParam trùng khớp với API /auto-register mới
            String url = String.format("http://localhost:8080/api/bids/auto-register?auctionId=%d&userId=%d&maxBid=%.2f",
                    itemId, Session.getUser().getId(), maxBid);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.noBody()) // Gửi Post thuần tham số, không Body JSON
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
                // Đọc thông tin đồng bộ trả về từ DTO AuctionUpdateResponse của Server
                JsonNode root = mapper.readTree(response.body());
                if (root.has("currentPrice")) {
                    currentPrice = root.path("currentPrice").asDouble(currentPrice);
                    lblCurrentHighest.setText(String.format("$%.2f", currentPrice));
                    lblMinBidAlert.setText(String.format("Set Your Maximum Bid Limit (Min: $%.2f)", currentPrice + bidIncrement));
                }
            } catch (Exception ignored) {
            }
            paneNotification.setVisible(true);
            paneNotification.setManaged(true);
            btnActivateAutoBid.setDisable(true); // Khóa nút sau khi cài đặt thành công
            appendRealtimeLog("Auto-bid successfully activated!");
            return;
        }

        // Trường hợp server trả lỗi (Mã 400 từ Handler)
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

    private void initWebSocketListener() {
        if (itemId == null) return;

        // 🌟 Gọi Singleton nhận tín hiệu Runnable đồng bộ với Service mới
        WebsocketConfigService.getInstance().subscribeAuctionRoom(itemId, () -> {
            // Đẩy về luồng chạy giao diện an toàn của JavaFX
            Platform.runLater(() -> {
                System.out.println("====== [AUTOBID SOCKET] Nhận tín hiệu! Tiến hành cập nhật lại giá và log...");

                // 1. Gọi hàm cập nhật lại thông tin phòng từ Server qua API thụ động
                refreshAuctionDataData();
            });
        });
    }

    // 🌟 Viết thêm một hàm nhỏ để lấy giá mới nhất từ API, né hoàn toàn bóc tách JSON Socket trực tiếp
    private void refreshAuctionDataData() {
        String url = "http://localhost:8080/api/bids/history/" + itemId;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode root = mapper.readTree(response.body());
                            if (root.isArray() && !root.isEmpty()) {
                                // Lấy phần tử đầu tiên (Lượt bid mới nhất trong lịch sử trả về)
                                JsonNode latestBid = root.get(0);

                                double bidAmount = latestBid.path("bidAmount").asDouble(currentPrice);
                                String bidderName = latestBid.path("bidderName").asText("Anonymous");

                                // Cập nhật giá lên giao diện
                                currentPrice = bidAmount;
                                lblCurrentHighest.setText(String.format("$%.2f", currentPrice));
                                lblMinBidAlert.setText(String.format("Set Your Maximum Bid Limit (Min: $%.2f)", currentPrice + bidIncrement));
                                txtMaxBidLimit.setPromptText(String.format("%.2f", currentPrice + bidIncrement));

                                // Đẩy dòng log mới lên màn hình AutoBid
                                String logMessage = String.format("Bid placed by %s for $%.2f", bidderName, bidAmount);
                                appendRealtimeLog(logMessage);
                            }
                        } catch (Exception ignored) {}
                    }
                }));
    }

//    private void applyAuctionUpdate(String body) {
//        try {
//            JsonNode root = mapper.readTree(body);
//
//            // Đồng bộ dữ liệu phòng thời gian thực qua Socket phát cho cả phòng
//            if (root.has("currentPrice")) {
//                currentPrice = root.path("currentPrice").asDouble(currentPrice);
//                lblCurrentHighest.setText(String.format("$%.2f", currentPrice));
//                lblMinBidAlert.setText(String.format("Set Your Maximum Bid Limit (Min: $%.2f)", currentPrice + bidIncrement));
//                txtMaxBidLimit.setPromptText(String.format("%.2f", currentPrice + bidIncrement));
//            }
//
//            // Xử lý thông điệp log đẩy lên giao diện
//            String message = "Auction updated";
//            if (root.has("latestBidderName") && root.has("bidAmount")) {
//                message = String.format("Bid placed by %s for $%.2f",
//                        root.path("latestBidderName").asText("Anonymous"),
//                        root.path("bidAmount").asDouble(0));
//            } else if (root.has("message")) {
//                message = root.path("message").asText(message);
//            }
//
//            appendRealtimeLog(message);
//        } catch (Exception ignored) {
//        }
//    }

    private void appendRealtimeLog(String message) {
        if (vboxBidList == null) {
            return;
        }
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

        vboxBidList.getChildren().add(0, row); // Luôn chèn hành động mới lên hàng đầu tiên
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

    // Tự động hủy lắng nghe khi tắt popup nếu cần thiết giải phóng bộ nhớ
    public void shutdown() {
        WebsocketConfigService.getInstance().unsubscribeAuctionRoom();
    }
}
