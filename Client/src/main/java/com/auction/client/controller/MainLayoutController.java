package com.auction.client.controller;

import com.auction.client.controller.seller.MyListingsUnderViewController;
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

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
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
    private Button homeButton;
    @FXML
    private Button electronicsButton;
    @FXML
    private Button vehicleButton;
    @FXML
    private Button artButton;
    @FXML
    private Region homeUnderline;
    @FXML
    private Region electronicsUnderline;
    @FXML
    private Region vehicleUnderline;
    @FXML
    private Region artUnderline;
    @FXML
    private BorderPane contentPane;
    @FXML
    private Button addItemButton;
    @FXML
    private Label userNameField;
    @FXML
    private ImageView avatar;
    private String currentTab = "LIVE_AUCTIONS";
    // Tracking state cho seller registration
    private static boolean sellerApplicationSubmitted = false;
    private static MainLayoutController instance;
    private StompSession stompSession;

    @FXML
    private void initialize() {
        UserResponse user = Session.getUser();
        if (user != null) {
            userNameField.setText(user.getName());
            AppContext.getInstance().setUserId(user.getId());
        }
        instance = this;

        //thêm MainLayoutController vào AppContext để đổi trang ở các Controller khác
        AppContext.getInstance().setMainLayoutController(this);
        WebsocketConfigService.getInstance().connect();
        AppEventBus.on("SELLER_APPROVED", (data) -> {
            Platform.runLater(() -> {
                Session.getUser().setSellerStatus("APPROVED");
                System.out.println("Chuyển màn hình cho ng đc đồng ý");
                checkStatusSellerUI("APPROVED");
            });
        });
        AppEventBus.on("SELLER_REJECTED", (data) -> {
            Platform.runLater(() -> {
                Session.getUser().setSellerStatus("REJECTED");
                System.out.println("Chuyển màn hình cho người bị từ chối!");
                checkStatusSellerUI("REJECTED");
            });
        });
        Platform.runLater(this::openDefaultCenterView);
    }

    private void openDefaultCenterView() {
        UserResponse user = Session.getUser();
        if (user != null && user.getRoles() != null && user.getRoles().contains("SELLER")) {
            openMyListingsView();
        } else {
            showLiveAuctionsView();
        }
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
        showMyBidsView(null);
    }

    public void showMyBidsView(String categoryFilter) {
        UserResponse user = Session.getUser();
        if (user == null) {
            Label placeholder = new Label("Session expired. Please login again.");
            placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: #523c34;");
            contentPane.setCenter(placeholder);
            return;
        }
        currentTab = "MY_BIDS";

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/auction/client/fxml/auction/HomeView.fxml"
                    )
            );

            Parent view = loader.load();
            HomeController controller = loader.getController();
            if (controller != null) {
                controller.setup(true, user.getId(), categoryFilter);
            }

            // đổi content
            setCenterView(view);

            // đổi màu tab active
            updateActiveTab(myBidsButton);
            updateCategoryTabByFilter(categoryFilter);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMyListingsLayout(ActionEvent event) {
        openMyListingsView();
    }

    private void openMyListingsView() {
        UserResponse currentUser = Session.getUser();
        if (currentUser == null) {
            Label placeholder = new Label("Session expired. Please login again.");
            placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: #523c34;");
            contentPane.setCenter(placeholder);
            return;
        }
        currentTab = "MY_LISTINGS";
        checkStatusSellerUI(currentUser.getSellerStatus());
        updateActiveTab(myListingsButton);
        updateCategoryTabByFilter(null);
    }
    private void checkStatusSellerUI(String status){
        UserResponse user = Session.getUser();
        if ((status == null || status.isBlank())
                && user != null
                && user.getRoles() != null
                && user.getRoles().contains("SELLER")) {
            status = Status.APPROVED.toString();
        }
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
        showLiveAuctionsView(null);
    }

    public void showLiveAuctionsView(String categoryFilter) {
        currentTab = "LIVE_AUCTIONS";
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/auction/client/fxml/auction/HomeView.fxml"
                    )
            );

            Parent liveAuctionView = loader.load();
            HomeController controller = loader.getController();
            if (controller != null) {
                controller.setup(false, null, categoryFilter);
            }

            // đổi content
            setCenterView(liveAuctionView);

            // đổi màu tab active
            updateActiveTab(liveAuctionsButton);
            updateCategoryTabByFilter(categoryFilter);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void handleLiveAuctionsLayout(ActionEvent event) {
        showLiveAuctionsView(null);
    }

    @FXML
    public void handleHomeCategoryClick(ActionEvent event) {
        handleCategoryClick(null, homeButton);
    }

    @FXML
    public void handleElectronicsCategoryClick(ActionEvent event) {
        handleCategoryClick("ELECTRONICS", electronicsButton);
    }

    @FXML
    public void handleVehicleCategoryClick(ActionEvent event) {
        handleCategoryClick("VEHICLE", vehicleButton);
    }

    @FXML
    public void handleArtCategoryClick(ActionEvent event) {
        handleCategoryClick("ART", artButton);
    }

    private void handleCategoryClick(String category, Button categoryButton) {
        updateCategoryTab(categoryButton);
        if ("LIVE_AUCTIONS".equals(currentTab)) {
            showLiveAuctionsView(category);
        } else if ("MY_BIDS".equals(currentTab)) {
            showMyBidsView(category);
        } else if ("MY_LISTINGS".equals(currentTab)) {
            MyListingsUnderViewController myListingsController = AppContext.getInstance().getMyListingsController();
            if (myListingsController != null) {
                myListingsController.loadSellerListings(category);
            }
        }
    }

    private void updateCategoryTab(Button activeButton) {
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: #f5eae4; -fx-font-size: 15; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 5 10 5;";
        String activeStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 5 10 5;";
        homeButton.setStyle(normalStyle);
        electronicsButton.setStyle(normalStyle);
        vehicleButton.setStyle(normalStyle);
        artButton.setStyle(normalStyle);
        activeButton.setStyle(activeStyle);
        homeUnderline.setStyle("-fx-background-color: transparent;");
        electronicsUnderline.setStyle("-fx-background-color: transparent;");
        vehicleUnderline.setStyle("-fx-background-color: transparent;");
        artUnderline.setStyle("-fx-background-color: transparent;");

        if (activeButton == homeButton) {
            homeUnderline.setStyle("-fx-background-color: white;");
        } else if (activeButton == electronicsButton) {
            electronicsUnderline.setStyle("-fx-background-color: white;");
        } else if (activeButton == vehicleButton) {
            vehicleUnderline.setStyle("-fx-background-color: white;");
        } else if (activeButton == artButton) {
            artUnderline.setStyle("-fx-background-color: white;");
        }
    }

    private void updateCategoryTabByFilter(String categoryFilter) {
        if (categoryFilter == null || categoryFilter.isBlank()) {
            updateCategoryTab(homeButton);
        } else if ("ELECTRONICS".equalsIgnoreCase(categoryFilter)) {
            updateCategoryTab(electronicsButton);
        } else if ("VEHICLE".equalsIgnoreCase(categoryFilter)) {
            updateCategoryTab(vehicleButton);
        } else if ("ART".equalsIgnoreCase(categoryFilter)) {
            updateCategoryTab(artButton);
        }
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
