package com.auction.client.controller;

import com.auction.client.controller.seller.BecomeASellerController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class LiveAuctionsController {
    @FXML
    private void openRegisterDialog(ActionEvent event)  {
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/become-seller.fxml"));
            Parent root = fxmlLoader.load();
            BecomeASellerController controller = fxmlLoader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Register as a Seller");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
