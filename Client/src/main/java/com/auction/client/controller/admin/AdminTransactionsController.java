package com.auction.client.controller.admin;

import com.auction.client.service.SceneService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminTransactionsController {
    private static final java.util.logging.Logger logger = Logger.getLogger(AdminManageUsersController.class.getName());

    @FXML
    private Button btnTransactions;
    @FXML
    private ImageView avatar;
    @FXML
    public void switchToManageUserButton(ActionEvent event){
        SceneService.changeScene(event, "/com/auction/client/fxml/Admin/AdminManageUsers.fxml");
    }
    // Tải lại hoặc chuyển hướng về giao diện Trang chủ Dashboard của Admin
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

            // Lấy Stage chính hiện tại để truyền vào controller xử lý đăng xuất đóng cửa sổ
            Stage mainStage = (Stage) btnTransactions.getScene().getWindow();
            controller.setMainStage(mainStage);

            Popup popup = new Popup();
            popup.getContent().add(root);
            popup.setAutoHide(true); // Tự động ẩn đi khi nhấn ra vùng ngoài popup

            ImageView avatar = (ImageView) event.getSource();
            // Tính toán vị trí hiển thị popup dựa trên vị trí con trỏ chuột
            double x = event.getScreenX() - 30;
            double y = event.getScreenY() + 15;
            popup.show(avatar.getScene().getWindow(), x, y);

        }
        catch(Exception e){
            logger.log(Level.SEVERE, "Gặp ngoại lệ khi khởi tạo hoặc hiển thị Popup đăng xuất của Admin.", e);
        }
    }
}
