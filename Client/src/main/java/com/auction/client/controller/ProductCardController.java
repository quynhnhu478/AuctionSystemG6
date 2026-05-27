package com.auction.client.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static com.auction.client.service.AlertService.showAlert;

public class ProductCardController {
    private static final Logger log = LoggerFactory.getLogger(ProductCardController.class);
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

    private Long itemId;

    private Timeline countdownTimeline;

    public void setData(Long itemId, String name, String description, String category, Double price, LocalDateTime startingTime, LocalDateTime endTime, String imagePathOrBase64) {
        this.itemId = itemId;
        this.lblItemName.setText(name);
        this.lblDescription.setText(description);
        this.lblCategory.setText(category);
        this.lblPrice.setText("$" + String.format("%.2f", price));
        //tải ảnh
        if(imagePathOrBase64 != null && !imagePathOrBase64.isEmpty()) {
            try{
                //nếu chuỗi nhập vào là chuỗi base64
                if(imagePathOrBase64.length() > 100){
                    //giải mã chuỗi base64 thành mảng byte nhị phân
                    byte[] imageBytes = Base64.getDecoder().decode(imagePathOrBase64);

                    //đưa byte vào luồng đọc của Javafx để chuyển thành image
                    ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                    imgProduct.setImage(new Image(bais));
                }
                //nếu là file cục bộ do client chọn thì ta lấy luôn URI để hiển thị
                else{
                    File file = new File(imagePathOrBase64);
                    if(file.exists()){
                        imgProduct.setImage(new Image(file.toURI().toString()));
                    }
                    else {
                        //nếu là link ảnh từ Server guửi về (HTTP URL)
                        imgProduct.setImage(new Image(imagePathOrBase64));
                    }
                }
            }catch(Exception e){
                showAlert(Alert.AlertType.ERROR, "image upload error", e.getMessage());
            }
        }

        //xử lý đếm ngược thời gian
        //nếu có đồng hồ cũ đang chạy thì tắt đi
        if(countdownTimeline != null){
            countdownTimeline.stop();
        }

        //khởi tạo Timeline lặp đi lặp lại
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            LocalDateTime now = LocalDateTime.now();

            if(now.isBefore(startingTime)){
                //chưa đến giờ đấu giá
                java.time.Duration duration = java.time.Duration.between(now, startingTime);
                long days = duration.toDays();
                long hours = duration.toHoursPart();
                long minutes = duration.toMinutesPart();

                lblTimeRemaining.setText(String.format("%dd %dh %dm", days, hours, minutes));
                lblStatus.setText("UPCOMING");
                lblStatus.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-padding: 2 6 2 6; -fx-background-radius: 3; -fx-font-size: 10");
            }
            else if(now.isAfter(endTime)){
                //hết giờ đấu giá
                lblTimeRemaining.setText("00h 00m 00s");
                lblStatus.setText("CLOSED");
                lblStatus.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-padding: 2 6 2 6; -fx-background-radius: 3; -fx-font-size: 10");
            }
            else{
                //Đang trong thời gian đấu giá -> tính khoảng cách thời gian còn lại
                long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);

                //Đổi số giây tổng thành Giờ:Phút:Giây trực quan
                long hours = totalSeconds/3600;
                long minutes = (totalSeconds%3600)/60;
                long seconds = totalSeconds%60;

                //In chữ lên giao diện
                lblTimeRemaining.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                lblStatus.setText("OPEN");
            }
        }));

        //cấu hình cho đồng hồ chạy vô hạn lần
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        //bật công tắc cho đồng hồ bắt đầu chạy
        countdownTimeline.play();
    }
}
