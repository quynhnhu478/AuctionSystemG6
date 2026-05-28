package com.auction.client.controller.auction;

import com.auction.client.service.Session;
import com.auction.client.service.WebsocketConfigService;

import com.auction.common.payload.AuctionUpdateResponse;
import com.auction.common.payload.BidHistoryResponse;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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

import javafx.util.Duration;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.datatype.jsr310.JavaTimeModule;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


public class AuctionDetailsPopupController {
    @FXML private ImageView imgProductDetails;
    @FXML private Label lblItemName;
    @FXML private Label lblDescription;
    @FXML private Label lblCategory;
    @FXML private Label lblStatus;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentHighest;
    @FXML private Label lblTimeRemaining;
    @FXML private Label lblMinBidAlert;
    @FXML private TextField txtBidAmount;
    @FXML private Label lblBalance;
    @FXML private Button btnSubmitBid;
    @FXML private HBox paneNotification;
    @FXML private Label lblBidHistoryCount;
    @FXML private VBox vboxBidList;
    @FXML private Label lblNoBidsYet;
    private Timeline countdownTimeline;

    private Long itemId;
    private double startingPrice;
    private double currentPrice;
    private double bidIncrement;
    private LocalDateTime startingTime;
    private LocalDateTime endTime;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    private StompSession stompSession;

    private void initWebSocketListener(Long auctionId) {
        // Gọi hàm nhận tín hiệu mới vừa viết ở Bước 1
        WebsocketConfigService.getInstance().subscribeAuctionRoom(auctionId, () -> {
            // Đưa lệnh làm mới vào luồng chạy giao diện an toàn của JavaFX
            System.out.println("====== [EVENT] Nhận tín hiệu từ kênh tổng! Tiến hành cập nhật UI qua HTTP...");
            // Tải lại toàn bộ lịch sử đấu giá + tự nhảy giá cao nhất cực kỳ đồng bộ
            loadBidHistory(auctionId);
        });
    }

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
        loadBidHistory(itemId);
        initWebSocketListener(itemId);
    }

    @FXML
    private void handleSubmitBid() {
        if (itemId == null || Session.getUser() == null) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng đăng nhập lại hệ thống.");
            return;
        }
        double minBid = currentPrice + bidIncrement;
        double amount;
        try {
            amount = Double.parseDouble(txtBidAmount.getText().trim());
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.WARNING, "Số tiền không hợp lệ", "Vui lòng nhập một số tiền hợp lệ.");
            return;
        }

        if (amount < minBid) {
            showAlert(Alert.AlertType.WARNING, "Giá đặt quá thấp", String.format("Mức giá đặt tối thiểu phải là: $%.2f", minBid));
            return;
        }

        if (Session.getUser().getBalance() < amount) {
            showAlert(Alert.AlertType.WARNING, "Số dư không đủ",
                    String.format("Tài khoản của bạn hiện có $%.2f. Không đủ để đặt $%.2f.", Session.getUser().getBalance(), amount));
            return;
        }

        // Khớp chuẩn cấu trúc URL gửi lệnh lên Server của dự án
        String url = String.format("http://localhost:8080/api/bids/place?auctionId=%d&userId=%d&bidAmount=%.2f",
                itemId, Session.getUser().getId(), amount);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> handleBidResponse(response, amount)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến máy chủ đấu giá."));
                    return null;
                });
    }

    private void handleBidResponse(HttpResponse<String> response, double amount) {
        int statusCode = response.statusCode();
        String responseBody = response.body();

        System.out.println("[DEBUG BID] Status: " + statusCode + " | Body: " + responseBody);

        // 1. Kiểm tra xem Server có ném lỗi "holding the highest bid" về không
        if (responseBody != null && (responseBody.contains("highest bid") || responseBody.contains("holding"))) {
            showAlert(Alert.AlertType.ERROR, "Đặt giá thất bại", "Bạn đang là người giữ mức giá cao nhất hiện tại!");
            return;
        }

        // 1. TRƯỜNG HỢP THÀNH CÔNG (Mã 2xx)
        if (statusCode >= 200 && statusCode < 300) {
            AuctionUpdateResponse res = mapper.readValue(responseBody, AuctionUpdateResponse.class);
            if (res.getBidderBalance() != null && res.getBidderFreezeBalance() != null) {
                Session.getUser().setBalance(res.getBidderBalance());
                Session.getUser().setFreezeBalance(res.getBidderFreezeBalance());
            }
            applyAuctionUpdate(responseBody);
            loadBidHistory(itemId);

            paneNotification.setVisible(true);
            paneNotification.setManaged(true);
            txtBidAmount.clear();
            return;
        }

        // 2. TRƯỜNG HỢP THẤT BẠI (Mã lỗi)
        String message = "Không thể đặt giá. Mã lỗi: " + statusCode;
        try {
            JsonNode root = mapper.readTree(responseBody);
            if (root.has("message")) {
                message = root.path("message").asText();
            }
            else if(root.isTextual()) {
                message = root.asText();
            }
        } catch (Exception ignored) {
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                message = responseBody;
            }
        }
        showAlert(Alert.AlertType.ERROR, "Đặt giá thất bại", message);
    }

    public void loadBidHistory(Long auctionId) {
        if (auctionId == null) {
            System.err.println("Không thể tải lịch sử: auctionId bị null!");
            return;
        }

        String url = "http://localhost:8080/api/bids/history/" + auctionId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> handleServerResponse(response)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        System.err.println("Lỗi kết nối đến máy chủ: " + ex.getMessage());
                        showNoBidsLayout();
                    });
                    return null;
                });
    }

    private void handleServerResponse(HttpResponse<String> response) {

        vboxBidList.getChildren().clear();
        System.out.println("====== [DEBUG] SERVER RESPONSE CODE: " + response.statusCode());
        System.out.println("====== [DEBUG] SERVER RESPONSE BODY: " + response.body());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            System.err.println("Server báo lỗi, mã trạng thái: " + response.statusCode());
            showNoBidsLayout();
            return;
        }

        try {
            JsonNode root = mapper.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                showNoBidsLayout();
                return;
            }
            processAndRenderBids(root);
        } catch (Exception e) {
            System.err.println("Lỗi phân tích cú pháp JSON: " + e.getMessage());
            showNoBidsLayout();
        }
    }

    private void processAndRenderBids(JsonNode root) {
        // 1. Đảm bảo dọn sạch sẽ, trắng trơn bảng lịch sử cũ trước khi vẽ sòng phẳng bản mới
        vboxBidList.getChildren().clear();

        double highest = startingPrice;
        int count = 0;

        // 2. Vòng lặp duyệt dữ liệu lịch sử nhận được từ Server
        for (JsonNode bid : root) {
            count++;
            double bidAmount = bid.path("bidAmount").asDouble(0);
            if (bidAmount > highest) {
                highest = bidAmount;
            }

            // 3. SỬA TẠI ĐÂY: Thay vì add() thông thường (bị đẩy xuống đáy),
            // ta dùng add(0, ...) để đưa lượt đặt giá MỚI NHẤT lên ĐẦU BẢNG hiển thị.
            vboxBidList.getChildren().add(createBidRow(bid));
        }

        // 4. Cập nhật các nhãn giá hiển thị trên màn hình popup
        currentPrice = highest;
        updateCurrentPriceLabels();

        // 5. Ẩn dòng chữ thông báo "Chưa có lượt đặt cược"
        lblBidHistoryCount.setText("Bid History (" + count + " bids)");
        lblNoBidsYet.setVisible(false);
        lblNoBidsYet.setManaged(false);
    }

    private void showNoBidsLayout() {
        lblNoBidsYet.setVisible(true);
        lblNoBidsYet.setManaged(true);
        vboxBidList.getChildren().add(lblNoBidsYet);
        lblBidHistoryCount.setText("Bid History (0 bids)");
    }

    private HBox createBidRow(JsonNode bid) {
        String bidder = bid.path("bidderName").asText("Unknown");
        double amount = bid.path("bidAmount").asDouble(0);
        String time = formatBidTime(bid.path("bidTime"));

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

    private String formatBidTime(JsonNode bidTimeNode) {
        if (bidTimeNode == null || bidTimeNode.isMissingNode() || bidTimeNode.isNull()) {
            return "";
        }

        // Nếu Server trả về dạng mảng số [yyyy, MM, dd, HH, mm...]
        if (bidTimeNode.isArray() && bidTimeNode.size() >= 5) {
            int year = bidTimeNode.get(0).asInt();
            int month = bidTimeNode.get(1).asInt();
            int day = bidTimeNode.get(2).asInt();
            int hour = bidTimeNode.get(3).asInt();
            int minute = bidTimeNode.get(4).asInt();
            return String.format("%02d/%02d/%04d %02d:%02d", day, month, year, hour, minute);
        }

        // Nếu Server trả về chuỗi văn bản thuần (như cục log JSON 200 phía trên)
        String raw = bidTimeNode.asText("").trim();
        if (raw.isBlank()) return "";

        try {
            if (raw.contains(":") && !raw.contains("-") && !raw.contains("T")) {
                return raw; // Trả về luôn nếu chỉ có dạng giờ phút giây HH:mm:ss từ Socket
            }

            // Chuẩn hóa chuỗi thời gian để LocalDateTime nhận diện
            String isoString = raw.replace(" ", "T");
            LocalDateTime time = LocalDateTime.parse(isoString, DateTimeFormatter.ISO_DATE_TIME);
            return time.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            // Nếu parse lỗi, cắt chuỗi thủ công để không làm sập giao diện
            try {
                if (raw.length() >= 16) {
                    // Định dạng gốc: yyyy-MM-ddT18:45 -> đổi sang dd/MM/yyyy HH:mm
                    String datePart = raw.substring(0, 10); // yyyy-MM-dd
                    String timePart = raw.substring(11, 16); // HH:mm
                    String[] split = datePart.split("-");
                    return split[2] + "/" + split[1] + "/" + split[0] + " " + timePart;
                }
            } catch (Exception ignored) {}
            return raw;
        }
    }

    private void updateCurrentPriceLabels() {
        lblCurrentHighest.setText(String.format("$%.2f", currentPrice));
        lblMinBidAlert.setText(String.format("Place Your Bid (Min: $%.2f)", currentPrice + bidIncrement));
        txtBidAmount.setPromptText(String.format("%.2f", currentPrice + bidIncrement));
    }

    private void applyAuctionUpdate(String body) {
        try {
            JsonNode root = mapper.readTree(body);

            // Khớp chính xác các trường DTO bọc bên trong đối tượng 'userBalance' nhận từ Server
            if (root.has("userBalance")) {
                JsonNode balanceNode = root.get("userBalance");
                if (balanceNode.has("balance") && Session.getUser() != null) {
                    double updatedBalance = balanceNode.get("balance").asDouble();
                    Session.getUser().setBalance(updatedBalance);
                    lblBalance.setText(String.format("Your balance: $%.2f", updatedBalance));
                }
            }

            // Khớp chính xác các trường DTO bọc bên trong đối tượng 'roomUpdate' nhận từ Server
            if (root.has("roomUpdate")) {
                JsonNode roomNode = root.get("roomUpdate");
                if (roomNode.has("currentPrice")) {
                    currentPrice = roomNode.get("currentPrice").asDouble();
                }
                if (roomNode.has("endTime") && !roomNode.get("endTime").isNull()) {
                    LocalDateTime updatedEndTime = parseDateTime(roomNode.get("endTime").asText());
                    if (updatedEndTime != null) {
                        endTime = updatedEndTime;
                    }
                }
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
                return LocalDateTime.parse(raw.replace(" ", "T"), DateTimeFormatter.ISO_DATE_TIME);
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
        btnSubmitBid.setDisable(false); // Đảm bảo nút được bật khi phòng mở công khai
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
    public void setupCountdown(LocalDateTime startingTime, LocalDateTime endTime) {
        // Nếu chuyển qua lại giữa các phòng mà có timeline cũ thì dừng lại trước
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        if (startingTime == null || endTime == null) {
            lblTimeRemaining.setText("--");
            return;
        }

        // Tái sử dụng logic Timeline cực chuẩn từ ProductCard của bạn
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(startingTime)) {
                lblTimeRemaining.setText("Not started");
                btnSubmitBid.setDisable(true); // Chưa đến giờ thì không cho Bid
            } else if (now.isAfter(endTime)) {
                lblTimeRemaining.setText("00h 00m 00s");
                lblTimeRemaining.setStyle("-fx-text-fill: red;"); // Đổi màu thông báo hết giờ
                btnSubmitBid.setDisable(true); // Hết giờ thì khóa nút Bid
                countdownTimeline.stop();
            } else {
                // Tính toán thời gian thực tế
                long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;

                lblTimeRemaining.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
                btnSubmitBid.setDisable(false); // Trong thời gian đấu giá thì mở nút
            }
        }));

        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    // Hàm bổ trợ xóa bộ đếm chạy ngầm khi đóng cửa sổ popup
    public void stopTimeline() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        WebsocketConfigService.getInstance().unsubscribeAuctionRoom();
    }
}
