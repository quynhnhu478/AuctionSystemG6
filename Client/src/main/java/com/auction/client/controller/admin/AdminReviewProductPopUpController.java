package com.auction.client.controller.admin;

import com.auction.common.payload.ItemResponse;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminReviewProductPopUpController {
    @FXML
    private ImageView imgProductReview;
    @FXML
    private Label lblProductTitle;
    @FXML
    private Label lblSellerName;
    @FXML
    private Label lblCategory;
    @FXML
    private Label lblStartingPrice;
    @FXML
    private Label lblDuration;
    @FXML
    private TextArea txtDescriptionReview;
    private Long currentItemId;
    private static final Logger logger = Logger.getLogger(ReviewSellerRequestController.class.getName());

    private static final String BASE_URL = "http://localhost:8080";

    public void initData(Long itemId) {
        this.currentItemId = itemId;

        // Tạo Task chạy ngầm để kéo dữ liệu chi tiết từ AdminController (Backend)
        Task<ItemResponse> task = new Task<>() {
            @Override
            protected ItemResponse call() throws Exception {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/admin/product/" + itemId))
                        .GET()
                        .header("Accept", "application/json")
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                logger.info("Mã phản hồi từ Server (dòng 74): " + response.statusCode());
                logger.info("Nội dung body: " + response.body());
                if (response.statusCode() == 200) {
                    try {
                        // Khởi tạo mapper qua Builder của Jackson 3
                        ObjectMapper mapper = JsonMapper.builder()
                                .addModule(new JavaTimeModule()) // Đúng package tools.jackson
                                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // Tránh lỗi lệch trường
                                .build();

                        return mapper.readValue(response.body(), ItemResponse.class);

                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "LỖI PARSE JSON từ phản hồi của Server:", e);
                        throw e;
                    }
                } else {
                    throw new RuntimeException("Failed to load registration details");
                }
            }
        };

        // Khi lấy được dữ liệu về, đổ lên các ô Input/Label và ImageView trên UI
        task.setOnSucceeded(e -> {

            ItemResponse data = task.getValue();

            logger.info("Data nhận được ở UI: " + data);
            logger.info("Name: " + data.getName());

            lblProductTitle.setText(data.getName());
            lblCategory.setText(data.getCategories().toString());
            lblSellerName.setText(data.getSellerName());
            lblStartingPrice.setText("$"+String.format("$%.2f", data.getPrice()));
            Duration duration = Duration.between(data.getStartingTime(), data.getEndTime());

            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();

            lblDuration.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));



            if (data.getImageUrl() != null) {
                String frontImageUrl = BASE_URL + data.getImageUrl();

                imgProductReview.setImage(new Image(frontImageUrl, true));
            }
            txtDescriptionReview.setText(data.getDescription());
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            logger.warning("Lỗi chạy ngầm khi đang nạp thông tin đơn đăng ký!");
            if (exception != null) {
                logger.log(Level.SEVERE, "Chi tiết ngoại lệ luồng ngầm:", exception);
            }
        });

        new Thread(task).start();
    }

}
