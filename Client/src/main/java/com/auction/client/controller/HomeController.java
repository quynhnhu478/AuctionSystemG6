package com.auction.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import com.auction.client.controller.auction.ProductCardController;
import javafx.scene.layout.VBox;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.io.IOException;

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

    @FXML
    public void initialize() {
        // loadItems được gọi từ setCategoryFilter() sau khi MainLayout load view
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

            String categoryFilter = currentCategoryFilter == null ? null : currentCategoryFilter.toUpperCase(Locale.ROOT);
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
                itemsPane.getChildren().add(createProductCard(item));
            }
            emptyLabel.setText("No items found for this category.");
            emptyLabel.setVisible(shown == 0);
            emptyLabel.setManaged(shown == 0);
        } catch (Exception e) {
            showError("Failed to parse items data");
        }
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
