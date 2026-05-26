package com.auction.client.controller;

import com.auction.client.service.AppContext;
import com.auction.client.service.AppEventBus;
import com.auction.client.service.Session;
import com.auction.client.service.WebsocketConfigService;
import com.auction.common.payload.UserResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

import javafx.stage.Popup;
import javafx.stage.Stage;
import com.auction.common.enums.Status;

import org.springframework.messaging.simp.stomp.StompSession;


import java.io.IOException;


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
    private BorderPane contentPane;
    @FXML
    private Button addItemButton;
    @FXML
    private Label userNameField;
    @FXML
    private ImageView avatar;
    // Tracking state cho seller registration
    private static boolean sellerApplicationSubmitted = false;
    private static MainLayoutController instance;
    private StompSession stompSession;

    @FXML
    private void initialize() {
        UserResponse user = Session.getUser();

        if (user != null) {
            userNameField.setText(user.getName());

        }
        instance = this;

        //thêm MainLayoutController vào AppContext để đổi trang ở các Controller khác
        AppContext.getInstance().setMainLayoutController(this);
        WebsocketConfigService.getInstance().connect();
        AppEventBus.on("SELLER_APPROVED", (data) ->{
            Platform.runLater(() -> {
                Session.getUser().setSellerStatus("APPROVED");
                System.out.println("Chuyển màn hình cho ng đc đồng ý");
                checkStatusSellerUI("APPROVED");
            });
        });
        AppEventBus.on("SELLER_REJECTED", (data) ->{
            Platform.runLater(() -> {
                Session.getUser().setSellerStatus("REJECTED");
                System.out.println("Chuyển màn hình cho người bị từ chối!");
                checkStatusSellerUI("REJECTED");
            });
        });
    }

    //Hàm để thay đổi Center bằng code Java
    public void setCenterView(Node node){
        contentPane.setCenter(node);
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    // Hàm dùng chung để đổi màu tab active thành vàng và tab khác thành trắng
    private void updateActiveTab(Button activeButton) {
        // Tạo style chuẩn cho các tab bình thường (Màu trắng)
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0; -fx-padding: 0 20 0 20;";
        // Style dành riêng cho tab đang được chọn (Màu vàng #dfb160)
        String activeStyle = "-fx-background-color: transparent; -fx-text-fill: #dfb160; -fx-font-size: 14; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0; -fx-padding: 0 20 0 20;";

        // Đặt lại style mặc định cho toàn bộ nút
        liveAuctionsButton.setStyle(normalStyle);
        myBidsButton.setStyle(normalStyle);
        myListingsButton.setStyle(normalStyle);

        // Kích hoạt màu vàng cho nút vừa bấm
        activeButton.setStyle(activeStyle);
    }

    // Hàm phụ trợ để tải và hoán đổi View ở Center ở mọi nơi
    public static void switchCenterView(String fxmlPath) {
        try {
            if (instance != null) {
                FXMLLoader loader = new FXMLLoader(MainLayoutController.class.getResource(fxmlPath));
                Parent view = loader.load();
                instance.contentPane.setCenter(view); // Thay thế vùng center
            }
            else{
                System.out.println("Error: MainLayoutController instance is null!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean hasAuctions() {
        // TODO: kiểm tra danh sách sản phẩm từ dữ liệu thật.
        // Hiện tại chưa có sản phẩm nào nên trả về false.
        return false;
    }

    @FXML
    private void handleMyBidsLayout(ActionEvent event) {
        Label placeholder = new Label("My Bids view is not implemented yet.");
        placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: #523c34;");
        contentPane.setCenter(placeholder);
        updateActiveTab(myBidsButton);
    }

    @FXML
    private void handleMyListingsLayout(ActionEvent event) {
        UserResponse currentUser = Session.getUser();
        if (currentUser == null){
            return;
        }
        String status = currentUser.getSellerStatus();
        checkStatusSellerUI(status);
        updateActiveTab(myListingsButton);
    }
    private void checkStatusSellerUI(String status){
        if (status == null){
            switchCenterView("/com/auction/client/fxml/seller/become-seller-view.fxml");
        }
        else if(status.equalsIgnoreCase(Status.PENDING.toString())){
            switchCenterView("/com/auction/client/fxml/seller/my-listings-under-review.fxml");
        }
        else if(status.equalsIgnoreCase(Status.APPROVED.toString())){
            switchCenterView("/com/auction/client/fxml/seller/my-listings-view.fxml");
        }
        else if(status.equalsIgnoreCase(Status.REJECTED.toString())){
            switchCenterView("/com/auction/client/fxml/seller/become-seller-view.fxml");
        }
    }

    public static void setSellerApplicationSubmitted(boolean submitted) {
        sellerApplicationSubmitted = submitted;
    }
    @FXML
    // Hiển thị trang Live Auctions
    public void showLiveAuctionsView() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/auction/client/fxml/auction/HomeView.fxml"
                    )
            );

            Parent liveAuctionView = loader.load();

            // đổi content
            setCenterView(liveAuctionView);

            // đổi màu tab active
            updateActiveTab(liveAuctionsButton);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void handleLiveAuctionsLayout(ActionEvent event) {
        showLiveAuctionsView();
    }

    //hàm mở nút logout
    @FXML
    private void OpenAccountPopUp(MouseEvent event){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/account/AccountPopup.fxml"));
            Node root = fxmlLoader.load();
            AccountPopupController controller = fxmlLoader.getController();
            Stage mainStage = (Stage) myBidsButton.getScene().getWindow();
            controller.setMainStage(mainStage);
            controller.setUserBalanceInput(Session.getUser().getBalance());
            Popup popup = new Popup();
            popup.getContent().add(root);
            popup.setAutoHide(true);
            ImageView avatar = (ImageView) event.getSource();
            double x = event.getScreenX() - 110;
            double y = event.getScreenY() + 20;
            popup.show(avatar.getScene().getWindow(), x, y);

        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}