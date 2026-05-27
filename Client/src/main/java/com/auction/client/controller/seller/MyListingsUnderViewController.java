package com.auction.client.controller.seller;

import com.auction.client.controller.MainLayoutController;
import com.auction.client.service.AppContext;
import com.auction.common.payload.ItemResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class MyListingsUnderViewController {
    @FXML
    private Button addItemButton;
    @FXML
    private Button viewLiveAuctionsButton;

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