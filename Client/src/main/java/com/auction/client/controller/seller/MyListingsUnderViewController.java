package com.auction.client.controller.seller;

import com.auction.client.controller.MainLayoutController;
import com.auction.client.service.AppEventBus;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MyListingsUnderViewController {
    @FXML
    private Button addItemButton;
    @FXML
    private Button viewLiveAuctionsButton;
    // hàm lắng nghe websocket

    @FXML
    private void openAddProductDialog(ActionEvent event) {
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/add-product-dialog.fxml"));
            Parent root = fxmlLoader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Product");
            // 3. Quan trọng: Thiết lập Modality để khóa cửa sổ chính lại
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    @FXML
    private void handleViewLiveAuctions(ActionEvent event) {

        MainLayoutController
                .getInstance()
                .showLiveAuctionsView();
    }
}