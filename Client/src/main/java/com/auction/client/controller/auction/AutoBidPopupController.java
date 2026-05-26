package com.auction.client.controller.auction;

import com.auction.common.payload.ItemResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AutoBidPopupController {

    @FXML
    private ImageView imgProduct;

    @FXML
    private Label lblProductName;

    @FXML
    private Label lblCurrentPrice;

    @FXML
    private Label lblBidIncrement;

    @FXML
    private TextField txtMaxBid;

    @FXML
    private TextField txtBidIncrement;

    // ĐÃ SỬA: Khớp chính xác với fx:id="createListingButton" trong FXML
    @FXML
    private Button createListingButton;

    // ĐÃ SỬA: Khớp chính xác với fx:id="cancelButton" trong FXML
    @FXML
    private Button cancelButton;

    @FXML
    private Label lblBidHistoryCount;

    @FXML
    private VBox vboxBidList;

    @FXML
    private Label lblNoBidsYet;

    private ItemResponse itemData;
    private int logCount = 0;
    private static final String SERVER_IMAGE_URL = "http://localhost:8080/uploads/items/";

    @FXML
    public void initialize() {
        setupNumericTextField(txtMaxBid);
        setupNumericTextField(txtBidIncrement);
    }

    /**
     * Nhận dữ liệu từ bên ngoài truyền sang
     */
    public void setAuctionData(ItemResponse item) {
        this.itemData = item;

        lblProductName.setText(item.getName());
        lblCurrentPrice.setText(String.format("$%,.2f", item.getPrice()));
        lblBidIncrement.setText(String.format("+$%,.2f", item.getBidIncrement()));

        if (txtBidIncrement != null) {
            txtBidIncrement.setText(String.valueOf(item.getBidIncrement()));
        }

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            try {
                String fullImageUrl = SERVER_IMAGE_URL + item.getImageUrl();
                Image image = new Image(fullImageUrl, true);
                imgProduct.setImage(image);
            } catch (Exception e) {
                System.out.println("Image loading error: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleStartAutoBid(ActionEvent event) {
        String maxBidText = txtMaxBid.getText().trim();
        String incrementText = txtBidIncrement.getText().trim();

        if (maxBidText.isEmpty() || incrementText.isEmpty()) {
            addLog("⚠️ Error: Please enter complete configuration information!", "#EF4444");
            return;
        }

        try {
            double maxBid = Double.parseDouble(maxBidText);
            double bidIncrement = Double.parseDouble(incrementText);

            if (maxBid <= itemData.getPrice()) {
                addLog("⚠️ Error: Maximum Price must be greater than Current Price!", "#EF4444");
                return;
            }
            if (bidIncrement <= 0) {
                addLog("⚠️ Error: Price jump must be greater than 0!", "#EF4444");
                return;
            }

            Long userId;
            if (com.auction.client.service.Session.getUser() != null) {
                userId = com.auction.client.service.Session.getUser().getId();
            } else {
                userId = 1L;
            }
            Long auctionId = itemData.getId();

            String url = String.format("http://localhost:8080/api/auction/autobid?userId=%d&auctionId=%d&maxBid=%f&bidIncrement=%f",
                    userId, auctionId, maxBid, bidIncrement);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                    .build();

            java.net.http.HttpClient.newHttpClient()
                    .sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            addLog(String.format(" Activation successful! Auto-Bid for [%s]", itemData.getName()), "#10B981");
                            addLog(String.format("   • Maximum limit: $%,.2f", maxBid), "#475569");
                            addLog(String.format("   • Configuration increment step: $%,.2f", bidIncrement), "#475569");

                            // Khóa các trường nhập liệu sau khi khởi chạy thành công
                            createListingButton.setDisable(true);
                            txtMaxBid.setDisable(true);
                            txtBidIncrement.setDisable(true);
                        } else {
                            addLog("⚠️ Activation failed: " + response.body(), "#EF4444");
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> addLog("⚠️ Connection error: " + ex.getMessage(), "#EF4444"));
                        return null;
                    });

        } catch (NumberFormatException e) {
            addLog("⚠️ Error: Invalid number format entered", "#EF4444");
        }
    }

    /**
     * Xử lý khi nhấn nút Cancel để đóng popup (onAction="#handleCancel")
     */
    @FXML
    void handleCancel(ActionEvent event) {
        // ĐVỚI BIẾN ĐÃ ĐƯỢC ĐỔI TÊN ĐỒNG BỘ: Hoạt động chính xác không còn lo lỗi NullPointerException
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    /**
     * Ghi Log hoạt động động vào VBox hiển thị trực quan
     */
    private void addLog(String message, String hexColor) {
        Platform.runLater(() -> {
            if (lblNoBidsYet != null && vboxBidList.getChildren().contains(lblNoBidsYet)) {
                vboxBidList.getChildren().remove(lblNoBidsYet);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            Label logLabel = new Label(String.format("[%s] %s", timestamp, message));
            logLabel.setTextFill(Color.web(hexColor));
            logLabel.setFont(Font.font("System", 13));
            logLabel.setWrapText(true);
            logLabel.setPadding(new Insets(2, 5, 2, 5));

            vboxBidList.getChildren().add(0, logLabel);

            logCount++;
            if (lblBidHistoryCount != null) {
                lblBidHistoryCount.setText(String.format("📈 Real-time Log (%d actions)", logCount));
            }
        });
    }

    private void setupNumericTextField(TextField textField) {
        if (textField != null) {
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.matches("\\d*(\\.\\d*)?")) {
                    textField.setText(oldValue);
                }
            });
        }
    }
}