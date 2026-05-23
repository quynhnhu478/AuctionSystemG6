package com.auction.client.controller.seller;

import com.auction.client.service.AppContext;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.auction.client.service.AlertService.showAlert;

public class CardItemController {
    @FXML
    private VBox itemCard;
    @FXML
    private Button deleteButton;
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

    private Long itemId;  //lưu Id sản phẩm để xóa

    public void setData(Long id, String title, String description, String category, double price, LocalDateTime startingTime, LocalDateTime endTime, String imagePathOrUrl){
        this.itemId = id;
        titleLabel.setText(title);
        descriptionLabel.setText(description);
        categoryLabel.setText(category);
        priceLabel.setText("$" + price);

        //Xử lý nạp ảnh
        if(imagePathOrUrl != null && !imagePathOrUrl.isEmpty()) {
            try{
                //nếu chuỗi chuồi vào là chuỗi base64
                if(imagePathOrUrl.length() > 100){
                    //giải mã chuỗi base64 thành mảng byte nhị phân
                    byte[] imageBytes = Base64.getDecoder().decode(imagePathOrUrl);

                    //đưa byte vào luồn đoọc của Javafx để chuyển thành image
                    ByteArrayInputStream  bais = new ByteArrayInputStream(imageBytes);
                    itemImageView.setImage(new Image(bais));
                }
                //nếu là file cục bộ do client chọn thì ta lấy luôn URI để hiển thị
                else{
                    File file = new File(imagePathOrUrl);
                    if(file.exists()){
                        itemImageView.setImage(new Image(file.toURI().toString()));
                    }
                    else {
                        //nếu là link ảnh từ Server guửi về (HTTP URL)
                        itemImageView.setImage(new Image(imagePathOrUrl));
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

    @FXML
    void handleDeleteButton(){
        if (statusLabel != null && "OPEN".equals(statusLabel.getText())) {
            showAlert(Alert.AlertType.WARNING, "Fail", "The product is currently being auctioned and cannot be deleted!");
            return;
        }

        if(itemId == null){
            showAlert(Alert.AlertType.ERROR, "Error", " Cannot delete item without an ID");
            return;
        }

        //tạo HttpClient gửi request DELETE
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http:/localhost:8080/api/items" + itemId)) //truyền id lên URL
                .header("Seller-ID", String.valueOf(AppContext.getInstance().getUserId()))
                .DELETE()
                .build();

        //Gửi bất đồng bộ
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if(response.statusCode() == 200 || response.statusCode() == 204){
                        //xóa trên giao diện (phải chạy trong Platform.runLater)
                        javafx.application.Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Item deleted successfully!");

                            ItemContainerController itemContainerController = AppContext.getInstance().getItemContainerController();
                            if(itemContainerController != null){
                                itemContainerController.refreshGridAfterDelete(itemCard);
                            }
                        });
                    } else{
                        javafx.application.Platform.runLater(() -> {
                            showAlert(Alert.AlertType.ERROR, "Server Error", "Failed to delete item!. Code: " + response.statusCode());
                        });
                    }
                }).exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Connection Error", "Could not connect to server" + ex.getMessage());
                    });
                    return null;
                });
    }
}
