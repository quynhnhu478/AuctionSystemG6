package com.auction.client.controller;

import com.auction.client.controller.auction.ProductCardController;
import com.auction.client.service.AppEventBus;
import com.auction.common.payload.ItemResponse;
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
import java.util.function.Consumer;

public class HomeController {
    @FXML
    private FlowPane itemsPane;
    @FXML
    private Label emptyLabel;
    @FXML
    private Label subTitleLabel;

    private String currentCategoryFilter;
    private int loadRequestId = 0;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private Consumer<Object> itemCreatedListener;

    @FXML
    public void initialize() {
        if (itemsPane == null) {
            return;
        }
        itemCreatedListener = data -> {
            if (data instanceof ItemResponse created) {
                Platform.runLater(() -> insertCreatedItem(created));
            }
        };
        AppEventBus.on("ITEM_CREATED", itemCreatedListener);
        itemsPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && itemCreatedListener != null) {
                AppEventBus.off("ITEM_CREATED", itemCreatedListener);
                itemCreatedListener = null;
            }
        });
    }

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

    private void loadItems() {
        if (itemsPane == null) {
            return;
        }
        final int requestId = ++loadRequestId;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/items"))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> Platform.runLater(() -> {
                    if (requestId == loadRequestId) {
                        renderItems(response);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError("Could not connect to server"));
                    return null;
                });
    }

    private void renderItems(HttpResponse<String> response) {
        if (itemsPane == null) {
            return;
        }
        itemsPane.getChildren().clear();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            showError("Failed to load items. Code: " + response.statusCode());
            return;
        }
        try {
            JsonNode root = mapper.readTree(response.body());
            if (!root.isArray()) {
                showError("Invalid response format from server");
                return;
            }

            String categoryFilter = normalizedFilter();
            Set<Long> seenIds = new HashSet<>();
            int shown = 0;
            for (JsonNode item : root) {
                long itemId = item.path("id").asLong(-1);
                if (itemId >= 0 && !seenIds.add(itemId)) {
                    continue;
                }
                String category = item.path("categories").asText("");
                if (categoryFilter != null && !categoryFilter.isBlank() && !categoryFilter.equalsIgnoreCase(category)) {
                    continue;
                }
                shown++;
                VBox card = createProductCard(item);
                if (itemId >= 0) {
                    card.setId("product-card-" + itemId);
                }
                itemsPane.getChildren().add(card);
            }
            emptyLabel.setText("No items found for this category.");
            emptyLabel.setVisible(shown == 0);
            emptyLabel.setManaged(shown == 0);
        } catch (Exception e) {
            showError("Failed to parse items data");
        }
    }

    private void insertCreatedItem(ItemResponse created) {
        if (itemsPane == null || itemsPane.getScene() == null || created.getId() == null) {
            return;
        }
        String categoryFilter = normalizedFilter();
        String category = created.getCategories() == null ? "" : created.getCategories().toString();
        if (categoryFilter != null && !categoryFilter.isBlank() && !categoryFilter.equalsIgnoreCase(category)) {
            return;
        }

        String nodeId = "product-card-" + created.getId();
        itemsPane.getChildren().removeIf(node -> nodeId.equals(node.getId()));
        VBox card = createProductCard(created);
        card.setId(nodeId);
        itemsPane.getChildren().add(0, card);
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);
    }

    private String normalizedFilter() {
        return currentCategoryFilter == null ? null : currentCategoryFilter.toUpperCase(Locale.ROOT);
    }

    private VBox createProductCard(JsonNode item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/auction/ProductCard.fxml"));
            VBox card = loader.load();
            ProductCardController controller = loader.getController();
            controller.bindFromJson(item);
            return card;
        } catch (Exception e) {
            e.printStackTrace();
            return createFallbackCard(item);
        }
    }

    private VBox createProductCard(ItemResponse item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/auction/ProductCard.fxml"));
            VBox card = loader.load();
            ProductCardController controller = loader.getController();
            controller.bindFromItemResponse(item);
            return card;
        } catch (Exception e) {
            e.printStackTrace();
            VBox card = new VBox(6);
            card.setPrefWidth(250);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #d9c9be; -fx-border-radius: 8; -fx-padding: 10;");
            card.getChildren().add(new Label(item.getName() == null ? "Unknown item" : item.getName()));
            return card;
        }
    }

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

    private void showError(String message) {
        if (itemsPane == null || emptyLabel == null) {
            return;
        }
        itemsPane.getChildren().clear();
        emptyLabel.setText(message);
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
    }

    @FXML
    public void switchToLogin(ActionEvent actionEvent) {
        try {
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/com/auction/client/fxml/signin/login.fxml"));
            Node source = (Node) actionEvent.getSource();
            source.getScene().setRoot(loginRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
