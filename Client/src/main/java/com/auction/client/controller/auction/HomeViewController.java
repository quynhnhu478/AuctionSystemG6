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
        
        if (node.has("id") && !node.get("id").isNull()) {
            item.setId(node.get("id").asLong());
        }
        
        if (node.has("name") && !node.get("name").isNull()) {
            item.setName(node.get("name").asText());
        } else {
            item.setName("Unnamed Item");
        }
        
        if (node.has("description") && !node.get("description").isNull()) {
            item.setDescription(node.get("description").asText());
        } else {
            item.setDescription("");
        }
        
        if (node.has("price") && !node.get("price").isNull()) {
            item.setPrice(node.get("price").asDouble());
        } else {
            item.setPrice(0.0);
        }
        
        if (node.has("bidIncrement") && !node.get("bidIncrement").isNull()) {
            item.setBidIncrement(node.get("bidIncrement").asDouble());
        } else {
            item.setBidIncrement(0.0);
        }
        
        if (node.has("startingTime") && !node.get("startingTime").isNull()) {
            try {
                item.setStartingTime(java.time.LocalDateTime.parse(node.get("startingTime").asText()));
            } catch (Exception e) {
                item.setStartingTime(java.time.LocalDateTime.now());
            }
        } else {
            item.setStartingTime(java.time.LocalDateTime.now());
        }
        
        if (node.has("endTime") && !node.get("endTime").isNull()) {
            try {
                item.setEndTime(java.time.LocalDateTime.parse(node.get("endTime").asText()));
            } catch (Exception e) {
                item.setEndTime(java.time.LocalDateTime.now().plusDays(1));
            }
        } else {
            item.setEndTime(java.time.LocalDateTime.now().plusDays(1));
        }
        
        if (node.has("categories") && !node.get("categories").isNull()) {
            try {
                item.setCategories(Categories.valueOf(node.get("categories").asText()));
            } catch (Exception e) {
                item.setCategories(Categories.ELECTRONICS);
            }
        } else {
            item.setCategories(Categories.ELECTRONICS);
        }

        JsonNode imageUrlNode = node.get("imageUrl");
        if (imageUrlNode != null && !imageUrlNode.isNull()) {
            item.setImageUrl(imageUrlNode.asText());
        } else {
            item.setImageUrl("no-image.jpg");
        }

        return item;
    }
}
