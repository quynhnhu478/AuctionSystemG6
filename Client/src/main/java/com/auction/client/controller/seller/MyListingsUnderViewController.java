package com.auction.client.controller.seller;

import com.auction.client.controller.MainLayoutController;
import com.auction.client.service.AppEventBus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyListingsUnderViewController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(MyListingsUnderViewController.class.getName());

    @FXML
    private Button addItemButton;
    @FXML
    private Button viewLiveAuctionsButton;

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
            logger.log(Level.SEVERE, "Gặp ngoại lệ khi khởi tạo ngữ cảnh cửa sổ popup dialog thêm sản phẩm.", e);
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