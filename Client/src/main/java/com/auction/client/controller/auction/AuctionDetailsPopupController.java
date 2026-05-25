package com.auction.client.controller.auction;

import com.auction.client.service.AppEventBus;
import com.auction.client.service.Session;
import com.auction.common.payload.ItemResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AuctionDetailsPopupController {

    @FXML private Label lblCurrentPrice;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnSubmitBid;

    private Long auctionId;
    private Long userId;

    @FXML
    public void initialize() {
        btnSubmitBid.setOnAction(event -> handlePlaceBid());

        // 🔥 ĐÃ FIX: Khai báo rõ ràng kiểu dữ liệu (Object data) để Java không báo lỗi Lambda
        AppEventBus.on("AUCTION_PRICE_UPDATED", (Object data) -> {
            if (data == null) return;
            String jsonPayload = (String) data;

            Platform.runLater(() -> {
                try {
                    if (auctionId != null && jsonPayload.contains("\"id\":" + auctionId)) {
                        String targetToken = "\"currentPrice\":";
                        int startIndex = jsonPayload.indexOf(targetToken) + targetToken.length();
                        int endIndex = jsonPayload.indexOf(",", startIndex);
                        if (endIndex == -1) {
                            endIndex = jsonPayload.indexOf("}", startIndex);
                        }

                        String priceStr = jsonPayload.substring(startIndex, endIndex).trim();
                        double newPrice = Double.parseDouble(priceStr);

                        lblCurrentPrice.setText(String.format("%,.0f VNĐ", newPrice));
                        System.out.println("➔ Popup [ID: " + auctionId + "] đồng bộ giá: " + newPrice);
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi phân tích JSON tại Popup: " + e.getMessage());
                }
            });
        });
    }

    public void setAuctionData(ItemResponse itemData) {
        if (itemData == null) return;
        this.auctionId = itemData.getId();
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", itemData.getPrice()));

        if (Session.getUser() != null) {
            this.userId = Session.getUser().getId();
        } else {
            this.userId = 1L;
        }
    }

    private void handlePlaceBid() {
        String amountText = txtBidAmount.getText().trim();
        if (amountText.isEmpty()) {
            showNotification("Lỗi", "Vui lòng nhập số tiền!");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(amountText);
            if (userId == null || auctionId == null) return;

            HttpClient client = HttpClient.newHttpClient();
            String url = String.format("http://localhost:8080/api/auction/bid?userId=%d&auctionId=%d&amount=%f",
                    userId, auctionId, bidAmount);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        Platform.runLater(() -> {
                            if (response.statusCode() == 200) {
                                showNotification("Thành công", "Bạn đã đặt giá thành công!");
                                txtBidAmount.clear();
                            } else {
                                showNotification("Đặt giá thất bại", response.body());
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showNotification("Lỗi kết nối", "Không thể kết nối Server!"));
                        return null;
                    });
        } catch (NumberFormatException e) {
            showNotification("Lỗi dữ liệu", "Số tiền phải là chữ số hợp lệ!");
        }
    }

    private void showNotification(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}