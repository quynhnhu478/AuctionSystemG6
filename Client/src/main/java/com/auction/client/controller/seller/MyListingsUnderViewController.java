package com.auction.client.controller.seller;

import com.auction.client.controller.MainLayoutController;
import com.auction.client.service.AppContext;
import com.auction.client.service.Session;
import com.auction.common.payload.UserResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;

public class MyListingsUnderViewController {
    @FXML
    private Button addItemButton;
    @FXML
    private Button viewLiveAuctionsButton;

    @FXML
    private VBox emptyStateVBox;
    @FXML
    private GridPane itemContainerGrid;
    @FXML
    private Button headerAddItemButton;

    private int currentColumn = 0;
    private int currentRow = 0;

    @FXML
    public void initialize() {
        AppContext.getInstance().setMyListingsController(this);
        // Kiểm tra xem các control của màn My Listings có tồn tại không
        if (itemContainerGrid != null && emptyStateVBox != null) {
            loadSellerListings();
        }
    }

    public void loadSellerListings() {
        UserResponse currentUser = Session.getUser();
        if (currentUser == null) {
            return;
        }
        Long sellerId = currentUser.getId();

        // Xóa sạch các thẻ card cũ
        itemContainerGrid.getChildren().clear();
        currentColumn = 0;
        currentRow = 0;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/items"))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        Platform.runLater(() -> {
                            try {
                                tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
                                tools.jackson.databind.JsonNode root = mapper.readTree(response.body());
                                if (root.isArray()) {
                                    boolean hasListings = false;
                                    for (tools.jackson.databind.JsonNode node : root) {
                                        long itemSellerId = node.path("sellerId").asLong(-1);
                                        if (itemSellerId == sellerId) {
                                            hasListings = true;
                                            Long id = node.path("id").asLong();
                                            String title = node.path("name").asText();
                                            String description = node.path("description").asText();
                                            String category = node.path("categories").asText();
                                            double price = node.path("price").asDouble();
                                            String startingTimeStr = node.path("startingTime").asText(null);
                                            String endTimeStr = node.path("endTime").asText(null);
                                            String imageUrl = node.path("imageUrl").asText();

                                            LocalDateTime startingTime = parseDateTime(startingTimeStr);
                                            LocalDateTime endTime = parseDateTime(endTimeStr);

                                            addNewCardToGrid(id, title, description, category, price, startingTime, endTime, imageUrl);
                                        }
                                    }

                                    if (hasListings) {
                                        emptyStateVBox.setVisible(false);
                                        emptyStateVBox.setManaged(false);
                                        itemContainerGrid.setVisible(true);
                                        itemContainerGrid.setManaged(true);
                                    } else {
                                        emptyStateVBox.setVisible(true);
                                        emptyStateVBox.setManaged(true);
                                        itemContainerGrid.setVisible(false);
                                        itemContainerGrid.setManaged(false);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(raw.replace(" ", "T"));
            } catch (Exception e) {
                return null;
            }
        }
    }

    private void addNewCardToGrid(Long id, String title, String description, String category, double price, LocalDateTime startingTime, LocalDateTime endTime, String localImagePath) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/card-item.fxml"));
            VBox itemCardNode = fxmlLoader.load();

            CardItemController cardController = fxmlLoader.getController();
            cardController.setData(id, title, description, category, price, startingTime, endTime, localImagePath);

            itemContainerGrid.add(itemCardNode, currentColumn, currentRow);

            currentColumn++;
            if (currentColumn > 3) {
                currentColumn = 0;
                currentRow++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshGridAfterDelete(VBox deletedCardNode) {
        loadSellerListings();
    }

    @FXML
    public void openAddProductDialog(ActionEvent event) {
        try {
            URL dialogUrl = getClass().getResource("/com/auction/client/fxml/seller/add-product-dialog.fxml");
            if (dialogUrl == null) {
                throw new IllegalStateException("Cannot find add-product-dialog.fxml");
            }

            FXMLLoader fxmlLoader = new FXMLLoader(dialogUrl);
            Parent root = fxmlLoader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Product");
            Stage owner = (Stage) ((Node) event.getSource()).getScene().getWindow();
            dialogStage.initOwner(owner);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Cannot open Add Item");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleViewLiveAuctions(ActionEvent event) {
        MainLayoutController.getInstance().showLiveAuctionsView();
    }
}
