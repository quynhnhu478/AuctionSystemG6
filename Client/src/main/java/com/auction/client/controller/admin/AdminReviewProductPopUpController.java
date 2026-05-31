package com.auction.client.controller.admin;

import com.auction.client.config.ApiConfig;
import com.auction.client.service.AlertService;
import com.auction.common.payload.ItemResponse;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
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
    private static final Logger logger = Logger.getLogger(AdminReviewProductPopUpController.class.getName());

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

    @FXML
    private Button btnCancelPopup;

    @FXML
    private Button btnRejectProduct;

    @FXML
    private Button btnApproveProduct;

    private Long currentItemId;
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @FXML
    public void initialize() {
        btnCancelPopup.setOnAction(event -> closeWindow());
        btnApproveProduct.setOnAction(event -> handleApprove());
        btnRejectProduct.setOnAction(event -> handleReject());
    }
    private Image toImage(String value) {
        if (value == null || value.isBlank()) return null;

        try {
            if (value.startsWith("http")) {
                return new Image(value, true);
            }
            if (value.startsWith("/")) {
                return new Image(ApiConfig.BASE_URL + value, true);
            }

            if (value.contains(".") && value.length() < 200) {
                return new Image(ApiConfig.BASE_URL + "/uploads/items/" + value, true);
            }

            byte[] bytes = java.util.Base64.getDecoder().decode(value);
            return new Image(new java.io.ByteArrayInputStream(bytes));
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot load product image", e);
            return null;
        }
    }
    public void initData(Long itemId) {
        this.currentItemId = itemId;
        logger.info("Initializing product review data for item ID: " + itemId);

        Task<ItemResponse> task = new Task<>() {
            @Override
            protected ItemResponse call() throws Exception {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ApiConfig.BASE_URL + "/api/admin/product/" + itemId))
                        .GET()
                        .header("Accept", "application/json")
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return objectMapper.readValue(response.body(), ItemResponse.class);
                } else {
                    throw new RuntimeException("Failed to load product details. Status: " + response.statusCode());
                }
            }
        };

        task.setOnSucceeded(event -> {
            ItemResponse data = task.getValue();
            if (data != null) {
                lblProductTitle.setText(data.getName());
                lblSellerName.setText(data.getSellerName() != null ? data.getSellerName() : "Unknown");
                lblCategory.setText(data.getCategories() != null ? data.getCategories().name() : "N/A");
                lblStartingPrice.setText(String.format("$%.2f", data.getPrice() != null ? data.getPrice() : 0.0));
                txtDescriptionReview.setText(data.getDescription());

                if (data.getStartingTime() != null && data.getEndTime() != null) {
                    Duration duration = Duration.between(data.getStartingTime(), data.getEndTime());
                    long hours = duration.toHours();
                    lblDuration.setText(hours + " Hours");
                } else {
                    lblDuration.setText("N/A");
                }

                String imageUrl = data.getImageUrl();
                if (data.getImageUrls() != null && !data.getImageUrls().isEmpty()) {
                    imageUrl = data.getImageUrls().get(0);
                }
                imgProductReview.setImage(toImage(imageUrl));
            }
        });

        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            logger.log(Level.SEVERE, "Failed to fetch product details", exception);
            Platform.runLater(() -> AlertService.showAlert(Alert.AlertType.ERROR, "Error", "Cannot load product details!"));
        });

        new Thread(task).start();
    }

    private void handleApprove() {
        logger.info("Admin approved product ID: " + currentItemId);
        AlertService.showAlert(Alert.AlertType.INFORMATION, "Success", "Product approved and published successfully!");
        closeWindow();
    }

    private void handleReject() {
        logger.info("Admin clicked reject for product ID: " + currentItemId);
        if (currentItemId == null) {
            AlertService.showAlert(Alert.AlertType.ERROR, "Error", "Invalid product ID!");
            return;
        }

        Task<HttpResponse<String>> task = new Task<>() {
            @Override
            protected HttpResponse<String> call() throws Exception {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ApiConfig.BASE_URL + "/api/items/" + currentItemId))
                        .DELETE()
                        .build();
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            }
        };

        task.setOnSucceeded(event -> {
            HttpResponse<String> response = task.getValue();
            if (response.statusCode() == 200 || response.statusCode() == 204) {
                AlertService.showAlert(Alert.AlertType.INFORMATION, "Success", "Product rejected and deleted successfully!");
                closeWindow();
            } else {
                AlertService.showAlert(Alert.AlertType.ERROR, "Error", "Failed to reject product! Code: " + response.statusCode());
            }
        });

        task.setOnFailed(event -> {
            logger.log(Level.SEVERE, "Failed to reject/delete product", task.getException());
            AlertService.showAlert(Alert.AlertType.ERROR, "Connection Error", "Cannot send request to server!");
        });

        new Thread(task).start();
    }

    private void closeWindow() {
        if (btnCancelPopup.getScene() != null && btnCancelPopup.getScene().getWindow() != null) {
            Stage stage = (Stage) btnCancelPopup.getScene().getWindow();
            stage.close();
        }
    }
}
