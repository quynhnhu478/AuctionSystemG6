package org.example.controllers;

import eu.hansolo.tilesfx.addons.Indicator;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import javax.swing.*;
import java.awt.event.MouseEvent;

public class MainController {
    @FXML
    private Label welcomeText;
    @FXML
    private Rectangle indicator;
    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }

    @FXML
    public void handleMouseEntered(javafx.scene.input.MouseEvent mouseEvent){
        Button btn = (Button) mouseEvent.getSource();

        // 1. Lấy tọa độ X tuyệt đối của Nút trên cửa sổ (Scene)
        double btnSceneX = btn.localToScene(0, 0).getX();

        // 2. Lấy tọa độ X tuyệt đối của vùng chứa thanh trượt (StackPane) trên cửa sổ
        double containerSceneX = indicator.getParent().localToScene(0, 0).getX();

        // 3. Vị trí trượt thực tế = Vị trí Nút - Vị trí Vùng chứa
        double targetX = btnSceneX - containerSceneX;

        // 4. Thực hiện trượt
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), indicator);
        tt.setToX(targetX);
        tt.play();

        // 5. Cập nhật các thuộc tính khác
        indicator.setWidth(btn.getWidth());
        indicator.setFill(Color.web("#212121"));
        indicator.setOpacity(1.0);
    }



}
