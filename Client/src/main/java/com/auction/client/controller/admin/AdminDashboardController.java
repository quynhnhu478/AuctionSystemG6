package com.auction.client.controller.admin;

import com.auction.client.controller.AccountPopupController;
import com.auction.client.service.SceneService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;


public class AdminDashboardController {
    @FXML
    private Button btnManageAuctions;
    @FXML
    private ImageView avatar;
    @FXML
    public void switchToManageUserButton(ActionEvent event){
        SceneService.changeScene(event, "/com/auction/client/fxml/Admin/AdminManageUsers.fxml");

    }
    @FXML
    public void switchToManageAuctionsButton(ActionEvent event){
        SceneService.changeScene(event, "/com/auction/client/fxml/Admin/AdminDashboard.fxml");
    }
    @FXML
    public void OpenLogoutDialog(MouseEvent event){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/Admin/AdminLogout.fxml"));
            Node root = fxmlLoader.load();
           AdminLogoutController controller = fxmlLoader.getController();
            Stage mainStage = (Stage) btnManageAuctions.getScene().getWindow();
            controller.setMainStage(mainStage);

            Popup popup = new Popup();
            popup.getContent().add(root);
            popup.setAutoHide(true);
            ImageView avatar = (ImageView) event.getSource();
            double x = event.getScreenX() -30;
            double y = event.getScreenY() + 15;
            popup.show(avatar.getScene().getWindow(), x, y);

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
