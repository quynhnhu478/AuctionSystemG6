package com.auction.client.controller.seller;

import com.auction.client.service.AppContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.auction.client.service.Session;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;
import javafx.application.Platform;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.auction.client.service.AlertService.showAlert;

public class ItemContainerController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(ItemContainerController.class.getName());
    private static String cachedItemsJson;
    private static String lastRenderedJson;
    @FXML
    private GridPane itemContainer;

    // Biến tọa độ toàn cục dùng để quản lý vị trí sắp xếp các ô (cell) trong lưới GridPane một cách chính xác
    private int currentColumn = 0;
    private int currentRow = 0;
    private String currentCategoryFilter;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new JsonMapper().builder()
            .addModule(new JavaTimeModule())
            .build();
    @FXML
    public void initialize() {
        AppContext.getInstance().setItemContainerController(this);
        loadMyListingsFromServer();
    }

    public void setCategoryFilter(String categoryFilter) {
        this.currentCategoryFilter = categoryFilter;

        Long sellerId = AppContext.getInstance().getUserId();
        if (sellerId == null && Session.getUser() != null) {
            sellerId = Session.getUser().getId();
            AppContext.getInstance().setUserId(sellerId);
        }

        if (sellerId == null) {
            return;
        }

        if (cachedItemsJson != null) {
            renderMyListingsJson(cachedItemsJson, sellerId);
            lastRenderedJson = cachedItemsJson;
        } else {
            loadMyListingsFromServer();
        }
    }

    public void refreshFromServer() {
        cachedItemsJson = null;
        lastRenderedJson = null;
        loadMyListingsFromServer();
    }

    private void loadMyListingsFromServer() {
        Long sellerId = AppContext.getInstance().getUserId();
        if (sellerId == null && Session.getUser() != null) {
            sellerId = Session.getUser().getId();
            AppContext.getInstance().setUserId(sellerId);
        }

        if (sellerId == null) {
            return;
        }

        Long finalSellerId = sellerId;

        // Hiện cache ngay lập tức nếu đã từng load rồi
        if (cachedItemsJson != null) {
            renderMyListingsJson(cachedItemsJson, finalSellerId);
            lastRenderedJson = cachedItemsJson;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/items"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        return;
                    }

                    String newJson = response.body();
                    cachedItemsJson = newJson;

                    // Nếu dữ liệu không đổi thì không clear + render lại nữa, tránh chập chờn
                    if (!newJson.equals(lastRenderedJson)) {
                        renderMyListingsJson(newJson, finalSellerId);
                        lastRenderedJson = newJson;
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() ->
                            logger.log(Level.SEVERE, "Cannot connect to server when loading listings", ex)
                    );
                    return null;
                });
    }
    private void renderMyListingsJson(String json, Long sellerId) {
        try {
            JsonNode root = mapper.readTree(json);
            if (!root.isArray()) {
                return;
            }

            itemContainer.getChildren().clear();
            currentColumn = 0;
            currentRow = 0;

            for (JsonNode item : root) {
                Long itemSellerId = item.path("sellerId").isMissingNode()
                        ? null
                        : item.path("sellerId").asLong();

                if (itemSellerId == null || !itemSellerId.equals(sellerId)) {
                    continue;
                }

                String category = item.path("categories").asText("");
                if (currentCategoryFilter != null
                        && !currentCategoryFilter.isBlank()
                        && !currentCategoryFilter.equalsIgnoreCase(category)) {
                    continue;
                }

                String imageUrl = item.path("imageUrl").asText("");
                if (item.has("imageUrls") && item.get("imageUrls").isArray() && !item.get("imageUrls").isEmpty()) {
                    imageUrl = item.get("imageUrls").get(0).asText(imageUrl);
                }
                imageUrl = normalizeImageUrl(imageUrl);
                LocalDateTime serverTime = item.has("serverTime") && !item.get("serverTime").isNull()
                        ? mapper.convertValue(item.get("serverTime"), LocalDateTime.class)
                        : null;
                addNewCardToGrid(
                        item.path("id").asLong(),
                        item.path("name").asText(""),
                        item.path("description").asText(""),
                        category,
                        item.path("price").asDouble(0),
                        item.path("bidIncrement").asDouble(0),
                        mapper.convertValue(item.get("startingTime"), LocalDateTime.class),
                        mapper.convertValue(item.get("endTime"), LocalDateTime.class),
                        imageUrl,
                        item.path("bidCount").asInt(0),
                        serverTime,
                        item.path("auctionStatus").asText("")
                );
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot render seller listings", e);
        }
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }
        if (imageUrl.startsWith("http")) {
            return imageUrl;
        }
        if (imageUrl.startsWith("/")) {
            return "http://localhost:8080" + imageUrl;
        }
        return "http://localhost:8080/uploads/items/" + imageUrl;
    }

    @FXML
    public void addNewCardToGrid(Long id, String title, String description, String category,
                                 double price, double bidIncrement,
                                 LocalDateTime startingTime, LocalDateTime endTime,
                                 String localImagePath, int bidCount,
                                 LocalDateTime serverTime, String auctionStatus) {
        try {
            // Tải thành phần giao diện khuôn mẫu (layout) cho thẻ sản phẩm (item card)
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/card-item.fxml"));
            VBox itemCardNode = fxmlLoader.load();

            // Đổ các dữ liệu thuộc tính vào các trường hiển thị của lớp điều khiển card ứng với view model
            CardItemController cardController = fxmlLoader.getController();
            cardController.setData(id, title, description, category, price, bidIncrement,
                    startingTime, endTime, localImagePath, bidCount, serverTime,auctionStatus);
            // Thêm nút thành phần card sản phẩm vào đúng vị trí tọa độ mục tiêu trong lưới một cách an toàn
            // Lưu ý: Đã sửa lại lỗi đảo ngược vị trí cấu trúc từ (currentRow, currentColumn) cho khớp với quy tắc chuẩn của GridPane
            itemContainer.add(itemCardNode, currentColumn, currentRow);

            // Tính toán vị trí ô tiếp theo cho lượt gán tọa độ sắp xếp kế tiếp
            currentColumn++;
            if (currentColumn > 3) {  // Giới hạn lưới tối đa 4 cột (Chỉ số index tương ứng: 0, 1, 2, 3)
                currentColumn = 0;    // Đưa con trỏ cột quay trở lại vị trí ô đầu tiên bên trái ngoài cùng
                currentRow++;         // Tăng chỉ số hàng để chuyển xuống dòng tiếp theo bên dưới
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Gặp lỗi phân tích thành phần cấu trúc bên trong khi đang vẽ các khung nhìn giao diện.", e);
            showAlert(Alert.AlertType.ERROR, "Load Error", "The item card could not be loaded: " + e.getMessage());
        }
    }

    // Khối quy tắc xử lý dồn hàng và tái cấu trúc lại vị trí các ô trong lưới sau khi xóa phần tử
    public void refreshGridAfterDelete(VBox deletedCardNode) {
        // Thu thập toàn bộ các instance card giao diện hiện tại đang nằm trong thành phần chứa active
        List<Node> remainingCards = new ArrayList<>(itemContainer.getChildren());

        // Loại bỏ phần tử node giao diện tương ứng với sản phẩm vừa bị xóa ra khỏi danh sách theo dõi
        remainingCards.remove(deletedCardNode);
        cachedItemsJson = null;
        lastRenderedJson = null;
        // Xóa sạch toàn bộ các layout thành phần giao diện cũ đang hiển thị trên lưới GridPane
        itemContainer.getChildren().clear();

        // Đặt lại các biến đếm tọa độ giám sát không gian vị trí ban đầu
        currentColumn = 0;
        currentRow = 0;

        // Tiến hành lặp và tái cấu trúc nén layout, thiết lập lại các chỉ số tọa độ mới cho các card còn lại
        for (Node card : remainingCards) {
            itemContainer.add(card, currentColumn, currentRow);

            currentColumn++;
            if (currentColumn > 3) {
                currentColumn = 0;
                currentRow++;
            }
        }

        logger.info("[UI] Các ô item trong container động đã được sắp xếp dồn hàng và tái hiển thị an toàn sau khi thay đổi thực thể.");

    }
    @FXML
    public void openAddProductDialog(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/auction/client/fxml/seller/add-product-dialog.fxml")
            );
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Product");
            dialogStage.initOwner(((Node) event.getSource()).getScene().getWindow());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot open Add Product dialog", e);
            showAlert(Alert.AlertType.ERROR, "Cannot open Add Item", e.getMessage());
        }
    }
}
