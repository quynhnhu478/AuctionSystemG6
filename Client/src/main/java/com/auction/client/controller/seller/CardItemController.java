package com.auction.client.controller.seller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static com.auction.client.service.AlertService.showAlert;

public class CardItemController {
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

    private Timeline countdownTimeline;

    public void setData(String title, String description, String category, double price, LocalDateTime startingTime, LocalDateTime endTime, String imagePathOrUrl){
        titleLabel.setText(title);
        descriptionLabel.setText(description);
        categoryLabel.setText(category);
        priceLabel.setText("$" + price);

        //Xử lý nạp ảnh
        if(imagePathOrUrl != null && !imagePathOrUrl.isEmpty()) {
            try{
                //nếu là file cục bộ do client chọn thì ta lấy luôn URI để hiển thị
                File file = new File(imagePathOrUrl);
                if(file.exists()){
                    itemImageView.setImage(new Image(file.toURI().toString()));
                }
                else {
                    //nếu là link ảnh từ Server guửi về (HTTP URL)
                    itemImageView.setImage(new Image(imagePathOrUrl));
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
                timeLabel.setText("Not Started");
                statusLabel.setText("UPCOMING");
                statusLabel.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-padding: 2 6 2 6; -fx-background-radius: 3; -fx-font-size: 10");
            }
            else if(now.isAfter(endTime)){
                //hết giờ đấu giá
                timeLabel.setText("00h 00m 00s");
                statusLabel.setText("CLOSED");
                statusLabel.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-padding: 2 6 2 6; -fx-background-radius: 3; -fx-font-size: 10");
            }
            else{
                //Đang trong thời gian đấu giá -> tính khoảng cách thời gian còn lại
                long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);

                //Đổi số giây tổng thành Giờ:Phút:Giây trực quan
                long hours = totalSeconds/3600;
                long minutes = (totalSeconds%3600)/60;
                long seconds = totalSeconds%60;

                //In chữ lên giao diện
                timeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                statusLabel.setText("OPEN");
            }
        }));

        //cấu hình cho đồng hồ chạy vô hạn lần
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        //bật công tắc cho đồng hồ bắt đầu chạy
        countdownTimeline.play();
    }
}
