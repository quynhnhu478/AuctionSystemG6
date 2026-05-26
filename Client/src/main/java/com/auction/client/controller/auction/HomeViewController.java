package com.auction.client.controller.auction;

import com.auction.common.enums.Categories;
import com.auction.common.payload.ItemResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.auction.client.service.AlertService.showAlert;

public class HomeViewController {
    @FXML
    private TilePane productGrid;
    @FXML
    private Label emptyStateLabel;

    private final ObjectMapper objectMapper = new JsonMapper().builder().addModule(new JavaTimeModule()).build();

    @FXML
    public void initialize() {
        loadProducts();
    }

    private void loadProducts() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/item/"))
                .GET()
                .build();

        HttpClient.newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> handleProductsResponse(response)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(
                            Alert.AlertType.ERROR,
                            "Connection error",
                            "Unable to load products from server.\nDetail: " + ex.getMessage()
                    ));
                    return null;
                });
    }

    private void handleProductsResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            showAlert(Alert.AlertType.ERROR, "Server error", "Error code: " + response.statusCode() + "\nDetail: " + response.body());
            return;
        }

        try {
            JsonNode items = objectMapper.readTree(response.body());
            productGrid.getChildren().clear();

            if (items.isEmpty()) {
                emptyStateLabel.setVisible(true);
                emptyStateLabel.setManaged(true);
                return;
            }

            emptyStateLabel.setVisible(false);
            emptyStateLabel.setManaged(false);

            for (JsonNode itemNode : items) {
                ItemResponse item = toItemResponse(itemNode);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/auction/ProductCard.fxml"));
                Parent card = loader.load();

                ProductCardController controller = loader.getController();
                controller.setProductData(item);

                productGrid.getChildren().add(card);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Parse error", "Unable to render product cards: " + e.getMessage());
        }
    }

    private ItemResponse toItemResponse(JsonNode node) {
        ItemResponse item = new ItemResponse();
        item.setId(node.get("id").asLong());
        item.setName(node.get("name").asText());
        item.setDescription(node.get("description").asText());
        item.setPrice(node.get("price").asDouble());
        item.setBidIncrement(node.get("bidIncrement").asDouble());
        item.setStartingTime(java.time.LocalDateTime.parse(node.get("startingTime").asText()));
        item.setEndTime(java.time.LocalDateTime.parse(node.get("endTime").asText()));
        item.setCategories(Categories.valueOf(node.get("categories").asText()));

        JsonNode imageUrlNode = node.get("imageUrl");
        if (imageUrlNode != null && !imageUrlNode.isNull()) {
            item.setImageUrl(imageUrlNode.asText());
        }

        return item;
    }
}
