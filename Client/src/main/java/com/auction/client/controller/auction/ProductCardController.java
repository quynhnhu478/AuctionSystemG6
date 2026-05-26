package com.auction.client.controller.auction;

import com.auction.common.payload.ItemResponse;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ProductCardController {

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

    private ItemResponse itemData;
    private Timeline countdownTimeline;
    private static final String SERVER_IMAGE_URL = "http://localhost:8080/uploads/items/";
    @FXML
    public void initialize() {
        // Lắng nghe sự kiện click vào nút Đặt Giá
        btnPlaceBid.setOnAction(event -> openAuctionDetailsPopup());

        // Lắng nghe sự kiện click vào nút Đặt Giá Tự Động
        btnAutoBid.setOnAction(event -> openAutoBidPopup());
    }

    // Hàm nhận dữ liệu sản phẩm từ Main/Home controller truyền vào để đổ lên giao diện Card
    public void setProductData(ItemResponse item) {
        this.itemData = item;

        // 1. Hiển thị thông tin text cơ bản
        if (lblItemName != null) {
            lblItemName.setText(item.getName());
        }
        if (lblDescription != null) {
            lblDescription.setText(item.getDescription());
        }
        if (lblCategory != null) {
            lblCategory.setText(item.getCategories() == null ? "Category" : item.getCategories().toString());
        }
        if (lblStatus != null) {
            lblStatus.setText("OPEN");
        }
        if (lblPrice != null) {
            lblPrice.setText(String.format("$%,.2f", item.getPrice()));
        }
        if (lblBidCount != null) {
            lblBidCount.setText("0");
        }

        // 2. Load ảnh từ Server qua URL (nếu có)
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            try {
                String fullImageUrl = SERVER_IMAGE_URL + item.getImageUrl();
                Image image = new Image(fullImageUrl, true); // true để load bất đồng bộ không bị khựng giao diện
                imgProduct.setImage(image);
            } catch (Exception e) {
                System.out.println("Error loading product image: " + e.getMessage());
            }
        }

        // 3. Khởi chạy đồng hồ đếm ngược thời gian kết thúc đấu giá
        startCountdown(item.getEndTime());
    }

    // TODO: Xử lý đếm ngược thời gian thực (Real-time countdown)
    private void startCountdown(LocalDateTime endTime) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();

            if (now.isAfter(endTime)) {
                lblTimeRemaining.setText("It's over");
                lblTimeRemaining.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;"); // Chuyển chữ đỏ
                btnPlaceBid.setDisable(true);
                btnAutoBid.setDisable(true);
                countdownTimeline.stop();
            } else {
                long diffInSeconds = ChronoUnit.SECONDS.between(now, endTime);

                long hours = diffInSeconds / 3600;
                long minutes = (diffInSeconds % 3600) / 60;
                long seconds = diffInSeconds % 60;

                lblTimeRemaining.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
            }
        }));

        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    //TODO: Mở popup chi tiết đấu giá (Bấm nút Place Bid)
    private void openAuctionDetailsPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/auction/AuctionDetailsPopup.fxml"));
            Parent root = loader.load();

            // Truyền dữ liệu sản phẩm sang cho Popup Controller xử lý tiếp
            AuctionDetailsPopupController popupController = loader.getController();
            popupController.setAuctionData(itemData);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL); // Khóa màn hình chính cho tới khi đóng popup
            popupStage.initStyle(StageStyle.UNDECORATED); // Bỏ thanh viền window
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to open AuctionDetailsPopup.fxml: " + e.getMessage());
        }
    }

    // Mở popup cấu hình Auto-Bid (Bấm nút Auto Bid)
    private void openAutoBidPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/auction/AutoBidPopup.fxml"));
            Parent root = loader.load();

            // Truyền dữ liệu sang AutoBid Popup Controller
            com.auction.client.controller.auction.AutoBidPopupController autoBidController = loader.getController();
            autoBidController.setAuctionData(itemData);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initStyle(StageStyle.UNDECORATED);
            popupStage.setScene(new Scene(root));
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to open AutoBidPopup.fxml: " + e.getMessage());
        }
    }
}
