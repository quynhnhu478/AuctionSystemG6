package com.auction.client.controller.seller;

import com.auction.client.config.ApiConfig;
import com.auction.client.service.AppContext;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.auction.client.service.AlertService.showAlert;

public class CardItemController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(CardItemController.class.getName());
    private String auctionStatus;
    @FXML
    private VBox itemCard;
    @FXML
    private Button deleteButton;
    @FXML
    private Button editButton;
    @FXML
    private ImageView itemImageView;
    @FXML
    private Label titleLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label bidCountLabel;
    private Timeline countdownTimeline;
    private int currentBidCount;
    private Long itemId;  // Lưu ID sản phẩm để phục vụ logic xóa và sửa sản phẩm
    private String currentCategory;
    private LocalDateTime currentStartTime;
    private LocalDateTime currentEndTime;
    private String currentImageBase64;
    private double currentBidIncrement;
    private long serverTimeOffsetSeconds = 0;

    private LocalDateTime nowFromServerClock() {
        return LocalDateTime.now().plusSeconds(serverTimeOffsetSeconds);
    }
    public void setData(Long id, String title, String description, String category,
                        double price, double bidIncrement,
                        LocalDateTime startingTime, LocalDateTime endTime,
                        String imagePathOrBase64, int bidCount,
                       LocalDateTime serverTime,
                        String auctionStatus) {

        // Lưu các trường dữ liệu một cách an toàn vào bộ nhớ cục bộ của controller để cache lại trạng thái
        this.itemId = id;
        this.currentBidCount = bidCount;
        if (serverTime != null) {
            serverTimeOffsetSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), serverTime);
        }
        this.auctionStatus = auctionStatus;
        this.currentBidIncrement = bidIncrement;
        this.currentCategory = category;
        this.currentStartTime = startingTime;
        this.currentEndTime = endTime;
        this.currentImageBase64 = imagePathOrBase64;

        // Đổ thông tin dữ liệu lên các nhãn (label) hiển thị của giao diện
        titleLabel.setText(title);
        descriptionLabel.setText(description);
        categoryLabel.setText(category);
        priceLabel.setText(String.format("$%.2f", price));
        bidCountLabel.setText(String.valueOf(bidCount));
        loadImage(imagePathOrBase64);

        // Xử lý logic giải mã và tải luồng dữ liệu hình ảnh động
        if (false && imagePathOrBase64 != null && !imagePathOrBase64.isEmpty()) {
            try {
                // Kiểm tra xem chuỗi tham số truyền vào có phải là cấu trúc mã hóa base64 thuần túy hay không
                if (imagePathOrBase64.length() > 100) {
                    // Giải mã chuỗi văn bản base64 thành mảng byte chứa dữ liệu nhị phân gốc
                    byte[] imageBytes = Base64.getDecoder().decode(imagePathOrBase64);

                    // Đưa mảng byte vào luồng đọc bộ nhớ để chuyển thành đối tượng Image hiển thị trong JavaFX
                    ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                    itemImageView.setImage(new Image(bais));
                }
                // Nếu là đường dẫn tệp cục bộ do client chọn, truy cập thông qua tham chiếu trực tiếp URI của hệ thống
                else {
                    File file = new File(imagePathOrBase64);
                    if (file.exists()) {
                        itemImageView.setImage(new Image(file.toURI().toString()));
                    }
                    else {
                        // Phương án dự phòng: Xử lý chuỗi như một liên kết HTTP URL trỏ tới tài nguyên của Server
                        itemImageView.setImage(new Image(imagePathOrBase64, true));
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Gặp lỗi khi liên kết luồng hình ảnh với các tham số tài nguyên của item card.", e);
                showAlert(Alert.AlertType.ERROR, "Image Upload Error", "Unable to read or parse item image data payload: " + e.getMessage());
            }
        }

        // Thiết lập đồng hồ đếm ngược thời gian thực để cập nhật giao diện
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        updateAuctionStatus();
        // Khởi tạo một Timeline lặp đi lặp lại với chu kỳ mỗi giây một lần
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateAuctionStatus()));

        // Cấu hình cho đồng hồ chạy vô hạn lần
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        // Bật công tắc cho đồng hồ bắt đầu chạy
        countdownTimeline.play();
    }
    public int getCurrentBidCount() {
        return currentBidCount;
    }

    private void loadImage(String imagePathOrBase64) {
        if (imagePathOrBase64 == null || imagePathOrBase64.isBlank()) {
            return;
        }

        try {
            if (imagePathOrBase64.startsWith("http")) {
                itemImageView.setImage(new Image(imagePathOrBase64, true));
                return;
            }

            if (imagePathOrBase64.startsWith("/")) {
                itemImageView.setImage(new Image(ApiConfig.BASE_URL + imagePathOrBase64, true));                return;
            }

            File file = new File(imagePathOrBase64);
            if (file.exists()) {
                itemImageView.setImage(new Image(file.toURI().toString()));
                return;
            }

            if (imagePathOrBase64.contains(".") && imagePathOrBase64.length() < 200) {
                itemImageView.setImage(new Image(ApiConfig.BASE_URL + "/uploads/items/" + imagePathOrBase64, true));                return;
            }

            byte[] imageBytes = Base64.getDecoder().decode(imagePathOrBase64);
            itemImageView.setImage(new Image(new ByteArrayInputStream(imageBytes)));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot load item card image.", e);
        }
    }

    @FXML
    void handleDeleteButton(){
        if (statusLabel != null && "OPEN".equals(statusLabel.getText())) {
            showAlert(Alert.AlertType.WARNING, "Action Prevented", "The product is currently being actively auctioned and cannot be deleted!");
            return;
        }

        if (itemId == null) {
            showAlert(Alert.AlertType.ERROR, "Identifier Fault", "Cannot initiate remote item purge request without a valid entity ID mapping.");
            return;
        }

        // Tạo HttpClient để chuẩn bị gửi request DELETE lên server
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.BASE_URL + "/api/items/" + itemId)) // Truyền ID sản phẩm lên URL endpoint
                .header("Seller-ID", String.valueOf(AppContext.getInstance().getUserId()))
                .DELETE()
                .build();

        // Gửi request bất đồng bộ (Asynchronous) sang phía hệ thống backend
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200 || response.statusCode() == 204) {
                        // Tiến hành xóa thẻ sản phẩm trên giao diện (bắt buộc phải chạy trong luồng Platform.runLater)
                        javafx.application.Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Item deleted successfully!");

                            ItemContainerController itemContainerController = AppContext.getInstance().getItemContainerController();
                            if (itemContainerController != null) {
                                itemContainerController.refreshGridAfterDelete(itemCard);
                            }
                        });
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Server Error",
                                    "Failed to delete item! Code: " + response.statusCode() + "\n" + response.body());                        });
                    }
                }).exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        logger.log(Level.SEVERE, "Lỗi kết nối mạng khi thực hiện cuộc gọi API xóa sản phẩm.", ex);
                        showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not establish pipeline communication link channel with the master system backend engine: " + ex.getMessage());
                    });
                    return null;
                });
    }

    @FXML
    void handleEditButton(){
        updateAuctionStatus();
        if (statusLabel != null && "CLOSED".equals(statusLabel.getText())) {
            showAlert(Alert.AlertType.WARNING, "Action Prevented", "Cannot edit this item because the auction has ended.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/add-product-dialog.fxml"));
            Parent formRoot = loader.load();

            // Lấy thực thể controller của form vừa được nạp lên
            AddProductController addProductController = loader.getController();

            // Chuyển tiếp toàn bộ dữ liệu hiện tại sang cho form chỉnh sửa và truyền kèm chính tham chiếu controller này
            addProductController.setEditData(
                    itemId,
                    titleLabel.getText(),
                    descriptionLabel.getText(),
                    currentCategory,
                    Double.parseDouble(priceLabel.getText().replace("$", "").trim()),
                    currentBidIncrement,
                    currentStartTime,
                    currentEndTime,
                    currentImageBase64,
                    this
            );

            // Khởi tạo và hiển thị form điền thông tin lên một cửa sổ dialog (Stage) mới
            Stage stage = new Stage();
            stage.setTitle("Edit Product Details");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(formRoot));
            stage.show();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Gặp ngoại lệ xung đột khi nạp UI Overlay và ánh xạ ngữ cảnh các cửa sổ dialog.", e);
            showAlert(Alert.AlertType.ERROR, "Context Loader Fault", "Cannot initialize or construct the product edit input overlay wizard: " + e.getMessage());
        }
    }
    private void updateAuctionStatus() {
        if ("FINISHED".equalsIgnoreCase(auctionStatus)) {
            timeLabel.setText("00h 00m 00s");
            statusLabel.setText("FINISHED");
            statusLabel.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #F57C00; -fx-border-color: #FFE082; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");

            return;
        }

        if ("PAID".equalsIgnoreCase(auctionStatus)) {
            timeLabel.setText("00h 00m 00s");
            statusLabel.setText("PAID");
            statusLabel.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #B71C1C; -fx-border-color: #EF9A9A; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");

            return;
        }

        if ("CANCELED".equalsIgnoreCase(auctionStatus)) {
            timeLabel.setText("00h 00m 00s");
            statusLabel.setText("CANCELED");
            statusLabel.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #0D47A1; -fx-border-color: #BBDEFB; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");

            return;
        }
        LocalDateTime now = nowFromServerClock();
        if (now.isBefore(currentStartTime)) {
            timeLabel.setText("Not Started");
            statusLabel.setText("UPCOMING");
            statusLabel.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-border-color: #FFE082; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
        } else if (now.isAfter(currentEndTime)) {
            timeLabel.setText("00h 00m 00s");
            statusLabel.setText("CLOSED");
            statusLabel.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-border-color: #FFCC80; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
        } else {
            long totalSeconds = ChronoUnit.SECONDS.between(now, currentEndTime);
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;

            timeLabel.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
            statusLabel.setText("OPEN");
            statusLabel.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; -fx-background-radius: 6; -fx-padding: 3 10 3 10; -fx-font-weight: bold; -fx-font-size: 11;");
        }
    }

    public void dispose() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

}

