package org.example.controllers;

import eu.hansolo.tilesfx.addons.Indicator;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.io.IOException;

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
    private StackPane contentArea;

    // 1. Biến toàn cục để ghi nhớ nút đã click
    private Button currentlySelectedButton;

    // Hàm dùng chung để di chuyển thanh trượt (Gom từ code của bạn)
    private void moveIndicator(Button btn) {
        if (btn == null) return;

        // Logic tính tọa độ tuyệt đối y hệt code bạn đã viết
        double btnSceneX = btn.localToScene(0, 0).getX();
        double containerSceneX = indicator.getParent().localToScene(0, 0).getX();
        double targetX = btnSceneX - containerSceneX;

        // Thực hiện hiệu ứng trượt
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), indicator);
        tt.setToX(targetX);
        tt.play();

        // Cập nhật các thuộc tính khác
        indicator.setWidth(btn.getWidth());
        indicator.setFill(Color.BLACK);
        indicator.setOpacity(1.0);
    }

    // 2. Khi di chuột vào bất kỳ nút nào
    @FXML
    public void handleMouseEntered(javafx.scene.input.MouseEvent event) {
        Button btn = (Button) event.getSource();
        moveIndicator(btn);
    }

    // 3. Khi click chọn (Thực sự chuyển trang)
    @FXML
    public void handleMouseClicked(javafx.scene.input.MouseEvent event) {
        currentlySelectedButton = (Button) event.getSource();
        moveIndicator(currentlySelectedButton);
        // Code chuyển FXML của bạn ở đây...
        String buttonText = currentlySelectedButton.getText();
        String fxmlPath = "";

        if (buttonText.equals("Browse Auctions")) {
            fxmlPath = "/org/example/fxml/browse_view.fxml";
        } else if (buttonText.equals("My Products")) {
            fxmlPath = "/org/example/fxml/my_products.fxml";
        } else if (buttonText.equals("Won Auctions")) {
            fxmlPath = "/org/example/fxml/won_auctions.fxml";
        }

        // 3. Thực hiện load
        try {
            Parent node = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 4. Khi chuột rời khỏi vùng Menu (HBox cha)
    @FXML
    public void handleMouseExited(javafx.scene.input.MouseEvent event) {
        // Trả thanh trượt về nút đã được chọn trước đó
        if (currentlySelectedButton != null) {
            moveIndicator(currentlySelectedButton);
        } else {
            // Nếu chưa từng bấm nút nào, có thể ẩn thanh trượt đi
            indicator.setOpacity(0);
        }
    }
//    @FXML
//    public void handleBrowseAuction(){
//        try{
//            Parent node = FXMLLoader.load(getClass().getResource("/org/example/fxml/browse_view.fxml"));
//            contentArea.getChildren().setAll(node);
//        }
//        catch (IOException e){
//            e.printStackTrace();
//        }
//    }



}
