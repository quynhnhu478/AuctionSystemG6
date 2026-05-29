package com.auction.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.auction.client.service.Session;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NotificationPopupController {

    @FXML private VBox notificationListContainer;
    @FXML private Label emptyLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        loadHistoricalNotifications(); // Mở popup ra thì tự động lấy lịch sử thông báo cũ từ DB về
    }

    // 1. Tải thông báo cũ từ Database thông qua API REST
    private void loadHistoricalNotifications() {
        if (Session.getUser() == null) return;
        Long userId = Session.getUser().getId();

        String url = "http://localhost:8080/api/notifications/user/" + userId;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        try {
                            JsonNode root = mapper.readTree(response.body());
                            Platform.runLater(() -> {
                                notificationListContainer.getChildren().clear();
                                if (root.isArray() && root.size() > 0) {
                                    emptyLabel.setVisible(false);
                                    emptyLabel.setManaged(false);
                                    for (JsonNode noti : root) {
                                        addNotificationRow(noti, false); // Nạp dòng thông báo cũ
                                    }
                                } else {
                                    emptyLabel.setVisible(true);
                                    emptyLabel.setManaged(true);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    // 2. Hàm thêm một hàng thông báo vào giao diện (Dùng chung cho cả tin cũ và tin Real-time từ Socket)
    public void addNotificationRow(JsonNode noti, boolean isNew) {
        Long notiId = noti.path("id").asLong();
        String type = noti.path("type").asText("");
        String message = noti.path("message").asText("");
        boolean isHandled = noti.path("handled").asBoolean(false);

        VBox row = new VBox(6);
        row.setStyle("-fx-padding: 10; -fx-border-color: #F1F5F9; -fx-border-width: 0 0 1 0; -fx-background-color: " + (isNew ? "#F8FAFC" : "white") + ";");

        Label lblMsg = new Label(message);
        lblMsg.setWrapText(true);
        lblMsg.setStyle("-fx-font-size: 12.5; -fx-text-fill: #334155;");
        row.getChildren().add(lblMsg);

        // Nếu người nhận là NGƯỜI THẮNG cuộc và chưa thanh toán -> Vẽ 2 nút xử lý giao dịch
        if ("WINNER_CONFIRM".equals(type) && !isHandled) {
            HBox actionBox = new HBox(10);
            Button btnPay = new Button("Thanh toán");
            btnPay.setStyle("-fx-background-color: #16A34A; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold; -fx-cursor: hand;");

            Button btnReject = new Button("Từ chối");
            btnReject.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: bold; -fx-cursor: hand;");

            Label lblStatus = new Label();
            lblStatus.setStyle("-fx-font-size: 11; -fx-font-style: italic;");

            btnPay.setOnAction(e -> handleAction(notiId, true, actionBox, lblStatus));
            btnReject.setOnAction(e -> handleAction(notiId, false, actionBox, lblStatus));

            actionBox.getChildren().addAll(btnPay, btnReject);
            row.getChildren().addAll(actionBox, lblStatus);
        } else if ("WINNER_CONFIRM".equals(type) && isHandled) {
            Label lblStatus = new Label("Giao dịch này đã được giải quyết xử lý");
            lblStatus.setStyle("-fx-font-size: 11; -fx-text-fill: #94A3B8; -fx-font-style: italic;");
            row.getChildren().add(lblStatus);
        }

        Platform.runLater(() -> {
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
            // Tin nhắn mới thì đẩy lên đầu danh sách (vị trí 0), tin cũ xếp sau
            notificationListContainer.getChildren().add(isNew ? 0 : notificationListContainer.getChildren().size(), row);
        });
    }

    // Gọi API phản hồi lệnh Chốt khi bấm nút
    private void handleAction(Long notiId, boolean accept, HBox actionBox, Label lblStatus) {
        String url = "http://localhost:8080/api/notifications/winner-confirm?notificationId=" + notiId + "&accept=" + accept;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).POST(HttpRequest.BodyPublishers.noBody()).build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    Platform.runLater(() -> {
                        if (response.statusCode() == 200) {
                            actionBox.setVisible(false);
                            actionBox.setManaged(false);
                            if (accept) {
                                lblStatus.setText("Đã xác nhận mua và chuyển khoản thành công!");
                                lblStatus.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                            } else {
                                lblStatus.setText("Bạn đã từ chối nhận tài sản này (Hủy kèo).");
                                lblStatus.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold;");
                            }
                        }
                    });
                });
    }
}