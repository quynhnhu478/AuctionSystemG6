package com.auction.client.controller.seller;

import com.auction.client.controller.MainLayoutController;
import com.auction.client.service.AppEventBus;
import com.auction.common.payload.ItemResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.function.Consumer;

public class MyListingsUnderViewController {
    @FXML
    private Button addItemButton;
    @FXML
    private Button viewLiveAuctionsButton;
    @FXML
    private VBox rootVBox;

    private Consumer<Object> itemCreatedListener;

    @FXML
    public void openAddProductDialog(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/add-product-dialog.fxml"));
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

    @FXML
    public void initialize() {
        if (rootVBox == null) {
            return;
        }
        // Listen for item created events so this view can insert the new card inline (push content down)
        itemCreatedListener = (data) -> {
            if (!(data instanceof ItemResponse)) return;
            ItemResponse created = (ItemResponse) data;
            Platform.runLater(() -> {
                // Skip if this view is no longer attached to a scene — prevents stale listeners
                // from mutating a detached node tree after the user navigated away.
                if (rootVBox.getScene() == null) return;
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/card-item.fxml"));
                    Parent cardNode = loader.load();
                    com.auction.client.controller.seller.CardItemController controller = loader.getController();
                    controller.setData(
                            created.getId(),
                            created.getName(),
                            created.getDescription(),
                            created.getCategories() != null ? created.getCategories().toString() : "",
                            created.getPrice() != null ? created.getPrice() : 0.0,
                            created.getBidIncrement() != null ? created.getBidIncrement() : 0.0,
                            created.getStartingTime(),
                            created.getEndTime(),
                            created.getImageUrl()
                    );

                    String cardId = "seller-card-" + created.getId();
                    rootVBox.getChildren().removeIf(n -> n != addItemButton && (n.getId() == null || cardId.equals(n.getId())));
                    cardNode.setId(cardId);
                    int insertIndex = rootVBox.getChildren().indexOf(addItemButton);
                    if (insertIndex >= 0) rootVBox.getChildren().add(insertIndex, cardNode);
                    else rootVBox.getChildren().add(cardNode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        };
        AppEventBus.on("ITEM_CREATED", itemCreatedListener);

        // Auto-unsubscribe when the view is removed from its scene, otherwise this controller
        // (and its rootVBox reference) leaks and every fired event mutates a detached tree.
        rootVBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && itemCreatedListener != null) {
                AppEventBus.off("ITEM_CREATED", itemCreatedListener);
                itemCreatedListener = null;
            }
        });
    }
}
