package com.auction.client.controller;

import com.auction.client.service.AlertService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Nạp giao diện bố cục chính (main-layout) từ tài nguyên hệ thống
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/fxml/seller/main-layout.fxml"));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Bắt sự kiện người dùng bấm vào nút đóng cửa sổ (dấu X) để thực hiện quy trình đăng xuất/thoát hệ thống an toàn
        primaryStage.setOnCloseRequest(event -> {
            // Ngăn chặn hành vi đóng cửa sổ mặc định để đợi người dùng xác nhận thông qua hộp thoại
            event.consume();
            Logout(primaryStage);
        });
    }

    // Hàm xử lý hiển thị hộp thoại xác nhận thoát ứng dụng và dọn dẹp tài nguyên
    public void Logout(Stage stage){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setContentText("Are you sure you want to logout?");

        // Nếu người dùng nhấn chọn OK, tiến hành đóng Stage để kết thúc chương trình
        if (alert.showAndWait().get() == ButtonType.OK) {
            stage.close();
        }
    }

    // Điểm khởi đầu chuẩn của một ứng dụng Java, kích hoạt vòng đời luồng xử lý JavaFX
    public static void main(String[] args) {
        launch(args);
    }
}