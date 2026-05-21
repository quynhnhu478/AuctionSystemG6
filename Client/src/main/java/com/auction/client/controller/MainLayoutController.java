package com.auction.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainLayoutController {
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private Button liveAuctionsButton;
    @FXML
    private Button myBidsButton;
    @FXML
    private Button myListingsButton;
    @FXML
    private Button becomeSellerButton;

    // Hàm dùng chung để xóa màu active cũ và đặt màu active mới
    private void updateActiveTab(Button activeButton) {
        // 1. Xóa class active-tab khỏi tất cả các nút
        liveAuctionsButton.getStyleClass().remove("active-tab");
        myBidsButton.getStyleClass().remove("active-tab");
        myListingsButton.getStyleClass().remove("active-tab");

        // 2. Thêm class active-tab vào nút vừa được bấm
        if (!activeButton.getStyleClass().contains("active-tab")) {
            activeButton.getStyleClass().add("active-tab");
        }
    }

    // Hàm phụ trợ để tải và hoán đổi View ở Center
    private void switchCenterView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            mainBorderPane.setCenter(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLiveAuctionsLayout(ActionEvent event) {
            switchCenterView("/com/auction/client/fxml/live-auctions-view.fxml");
            updateActiveTab(liveAuctionsButton);
    }

    @FXML
    private void handleMyListingsLayout(ActionEvent event) {
            switchCenterView("/com/auction/client/fxml/my-listings-view.fxml");
            updateActiveTab(myListingsButton);
    }
    @FXML
    private void BecomeSellerPopUp(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/become-seller.fxml"));
            Parent root = fxmlLoader.load();

            Stage popupStage = new Stage();
            popupStage.setTitle("Pop up screen");
            popupStage.setScene(new Scene(root));

            popupStage.initModality(Modality.APPLICATION_MODAL);


            Stage mainStage = (Stage) becomeSellerButton.getScene().getWindow();
            popupStage.initOwner(mainStage);


            popupStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
