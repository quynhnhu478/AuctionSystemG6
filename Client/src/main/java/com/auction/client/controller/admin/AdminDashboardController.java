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

import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminDashboardController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(AdminDashboardController.class.getName());

    @FXML
    private Button btnManageAuctions;
    @FXML
    private ImageView avatar;

    // Chuyển hướng sang giao diện quản lý người dùng của Admin
    @FXML
    public void switchToManageUserButton(ActionEvent event){
        SceneService.changeScene(event, "/com/auction/client/fxml/Admin/AdminManageUsers.fxml");
    }

    // Tải lại hoặc chuyển hướng về giao diện Trang chủ Dashboard của Admin
    @FXML
    public void switchToManageAuctionsButton(ActionEvent event){
        SceneService.changeScene(event, "/com/auction/client/fxml/Admin/AdminDashboard.fxml");
    }

    // Xử lý sự kiện nhấn vào ảnh đại diện để hiển thị cửa sổ nhỏ (Popup) đăng xuất tài khoản Admin
    @FXML
    public void OpenLogoutDialog(MouseEvent event){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/Admin/AdminLogout.fxml"));
            Node root = fxmlLoader.load();
            AdminLogoutController controller = fxmlLoader.getController();

            // Lấy Stage chính hiện tại để truyền vào controller xử lý đăng xuất đóng cửa sổ
            Stage mainStage = (Stage) btnManageAuctions.getScene().getWindow();
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