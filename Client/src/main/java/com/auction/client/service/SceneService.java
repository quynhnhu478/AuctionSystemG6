package com.auction.client.service;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SceneService {
    // Khởi tạo Logger cho class SceneService
    private static final Logger log = LoggerFactory.getLogger(SceneService.class);

    public static void changeScene(ActionEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(SceneService.class.getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            // Ghi log ở level ERROR kèm theo thông điệp và toàn bộ Stack Trace của ngoại lệ e
            log.error("Khong the chuyen trang: {}", fxmlPath, e);
        }
    }
}