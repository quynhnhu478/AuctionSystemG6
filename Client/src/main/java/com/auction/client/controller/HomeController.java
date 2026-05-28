package com.auction.client.controller;

import com.auction.client.controller.auction.ProductCardController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HomeController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(HomeController.class.getName());

    @FXML
    private FlowPane itemsPane;
    @FXML
    private Label emptyLabel;
    @FXML
    private Label subTitleLabel;
    @FXML
    private Label welcomeText;

    private String currentCategoryFilter;
    private int loadRequestId = 0;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static String cachedItemsJson;
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        // loadItems được gọi từ setCategoryFilter() sau khi MainLayout load view
    }

    // Thiết lập bộ lọc danh mục sản phẩm và cập nhật lại tiêu đề giao diện
    public void setCategoryFilter(String category) {
        this.currentCategoryFilter = category;
        if (subTitleLabel != null) {
            if (category == null || category.isBlank()) {
                subTitleLabel.setText("Latest items from all categories");
            } else {
                subTitleLabel.setText("Category: " + category);
            }
        }
        if (itemsPane != null) {
            loadItems();
        }
    }

    // Gửi yêu cầu lấy danh sách sản phẩm đấu giá bất đồng bộ từ Server backend
    private void loadItems() {
        if (itemsPane == null) {
            return;
        }

        if (cachedItemsJson != null) {
            renderItemsJson(cachedItemsJson);
        } else {
            emptyLabel.setText("Loading items...");
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
        }

        final int requestId = ++loadRequestId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/items"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (requestId != loadRequestId) {
                        return;
                    }

                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        cachedItemsJson = response.body();
                        renderItemsJson(cachedItemsJson);
                    } else if (cachedItemsJson == null) {
                        showError("Failed to load items. Code: " + response.statusCode());
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (cachedItemsJson == null) {
                            showError("Could not connect to server");
                        }
                    });
                    return null;
                });
    }

    // Phân tích dữ liệu JSON nhận được từ Server và kết xuất ra các card item tương ứng
    private void renderItemsJson(String json) {
        if (itemsPane == null) {
            return;
        }
        itemsPane.getChildren().clear();
        try {
            JsonNode root = mapper.readTree(json);
            if (!root.isArray()) {
                showError("Invalid response format from server");
                return;
            }

            String categoryFilter = currentCategoryFilter == null ? null : currentCategoryFilter.toUpperCase(Locale.ROOT);
            Set<Long> seenIds = new HashSet<>();
            int shown = 0;
            for (JsonNode item : root) {
                long itemId = item.path("id").asLong(-1);
                // Kiểm tra loại bỏ trùng lặp ID sản phẩm nếu có
                if (itemId >= 0 && !seenIds.add(itemId)) {
                    continue;
                }
                String category = item.path("categories").asText("");
                // Lọc bỏ qua các sản phẩm không khớp với danh mục đang chọn
                if (categoryFilter != null && !categoryFilter.isBlank() && !categoryFilter.equalsIgnoreCase(category)) {
                    continue;
                }
                shown++;
                itemsPane.getChildren().add(createProductCard(item));
            }
            emptyLabel.setText("No items found for this category.");
            emptyLabel.setVisible(shown == 0);
            emptyLabel.setManaged(shown == 0);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Gặp lỗi khi phân tích cú pháp dữ liệu danh sách sản phẩm.", e);
            showError("Failed to parse items data");
        }
    }

    // Khởi tạo thẻ card sản phẩm chuẩn bằng cách nạp tệp cấu hình FXML và liên kết dữ liệu JSON
    private VBox createProductCard(JsonNode item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/auction/ProductCard.fxml"));
            VBox card = loader.load();
            ProductCardController controller = loader.getController();
            controller.bindFromJson(item);
            return card;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Không thể tải cấu trúc ProductCard.fxml, tự động chuyển sang card dự phòng.", e);
            return createFallbackCard(item);
        }
    }

    // Tạo card giao diện dự phòng bằng mã thuần Java khi việc nạp file FXML gặp sự cố
    private VBox createFallbackCard(JsonNode item) {
        VBox card = new VBox(6);
        card.setPrefWidth(250);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #d9c9be; -fx-border-radius: 8; -fx-padding: 10;");

        String name = item.path("name").asText("Unknown item");
        String desc = item.path("description").asText("");
        String category = item.path("categories").asText("N/A");
        String price = item.path("price").asText("0");

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #3f2f29;");
        Label categoryLabel = new Label("Category: " + category);
        categoryLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #5f5a57;");
        Label priceLabel = new Label("Price: $" + price);
        priceLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #2f855a;");
        Label descLabel = new Label(desc.isBlank() ? "-" : desc);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #6a6a6a;");

        card.getChildren().addAll(nameLabel, categoryLabel, priceLabel, descLabel);
        return card;
    }

    // Dọn sạch màn hình lưới và hiển thị thông báo lỗi trực quan cho người dùng
    private void showError(String message) {
        if (itemsPane == null || emptyLabel == null) {
            return;
        }
        itemsPane.getChildren().clear();
        emptyLabel.setText(message);
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
    }

    // Chuyển hướng người dùng quay trở lại cửa sổ giao diện Đăng nhập
    @FXML
    public void switchToLogin(ActionEvent actionEvent) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/auction/client/fxml/signin/login.fxml"));
            Node source = (Node) actionEvent.getSource();
            source.getScene().setRoot(loginRoot);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Gặp lỗi IO khi tải khung nhìn login.fxml để chuyển màn hình.", e);
        }
    }

}