package com.auction.client.controller.seller;

import com.auction.client.controller.MainLayoutController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Stage;

public class ApplicationSubmittedDialogController {

    private Stage registerSellerStage;
    private Stage becomeSellerStage;
    private MainLayoutController mainLayoutController;

    public void setParentStages(Stage registerStage, Stage becomeStage) {
        this.registerSellerStage = registerStage;
        this.becomeSellerStage = becomeStage;
    }

    public void setMainLayoutController(MainLayoutController mainLayoutController) {
        this.mainLayoutController = mainLayoutController;
    }

    @FXML
    private void handleClose(ActionEvent event) {
        // Set trạng thái seller application đã được submit
        MainLayoutController.setSellerApplicationSubmitted(true);

        // Yêu cầu MainLayout hiển thị và thay đổi hành vi các nút bấm trỏ sang trang mới
        if (mainLayoutController != null) {
            mainLayoutController.showAuctionHomeFromListings();
        } else {
            MainLayoutController main = MainLayoutController.getInstance();
            if (main != null) {
                main.showAuctionHomeFromListings();
            }
        }

        // Đóng các Stage hiện tại
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();

        if (registerSellerStage != null) {
            registerSellerStage.close();
        }
        if (becomeSellerStage != null) {
            becomeSellerStage.close();
        }
    }
}