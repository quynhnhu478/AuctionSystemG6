package com.auction.client.controller;

import com.auction.client.service.AppContext;
import com.auction.client.service.AppEventBus;
import com.auction.client.service.Session;
import com.auction.common.payload.UserResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import com.auction.common.enums.Status;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

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
    private ImageView bellImageView;
    @FXML
    private Label notificationBadgeLabel;

    //theo dõi thông báo ở nút notification
    private int notificationCount = 0;
    private final List<String> notificationList = new ArrayList<>();  //Nơi lưu trữ tạm cac tin từ Socket đổ về

    // Tracking state cho seller registration
    private static boolean sellerApplicationSubmitted = false;
    private static MainLayoutController instance;
    private StompSession stompSession;

    @FXML
    private void initialize() {
        instance = this;

        //thêm MainLayoutController vào AppContext để đổi trang ở các Controller khác
        AppContext.getInstance().setMainLayoutController(this);
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
        // Đặt tất cả nút về màu trắng
        liveAuctionsButton.setStyle(liveAuctionsButton.getStyle().replaceAll("-fx-text-fill:[^;]*;?", "") + "-fx-text-fill: white;");
        myBidsButton.setStyle(myBidsButton.getStyle().replaceAll("-fx-text-fill:[^;]*;?", "") + "-fx-text-fill: white;");
        myListingsButton.setStyle(myListingsButton.getStyle().replaceAll("-fx-text-fill:[^;]*;?", "") + "-fx-text-fill: white;");

        // Đặt nút active thành màu vàng
        activeButton.setStyle(activeButton.getStyle().replaceAll("-fx-text-fill:[^;]*;?", "") + "-fx-text-fill: #dfb160;");
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
    public void handleLiveAuctionsLayout(ActionEvent event) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/auction/client/fxml/seller/item-container-view.fxml"
                    )
            );

            Parent liveAuctionView = loader.load();

            setCenterView(liveAuctionView);
            updateActiveTab(liveAuctionsButton);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void connectAndListenWebSocket(){
        Long curenntUserId =  Session.getUser().getId();
        String topic = "/topic/user-" +curenntUserId;

        stompSession.subscribe(topic, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class; // Nhận phản hồi từ Server dạng String
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = (String) payload;

                // Lưu ý: Muốn sửa giao diện JavaFX từ Socket chạy ngầm bắt buộc phải bọc trong Platform.runLater
                Platform.runLater(() -> {

                    // Nếu Server báo đã duyệt thành Seller thành công
                    if ("ROLE_UPDATED_TO_SELLER".equals(message)) {
                        // Bắn thêm Event nội bộ thông báo cho các màn hình con (nếu cần)
                        AppEventBus.emit("SELLER_APPROVED", null);
                    }

                });
            }
        });
    }

    //Hàm này để WebSocket gọi để thêm tin nhắn mới
    public void addNewNotification(String message){
        notificationCount++;
        //chèn tin nhắn mới lên đầu danh sách để hiển thị ưu tiên trước
        notificationList.add(0, message);

        notificationBadgeLabel.setText(String.valueOf(notificationCount));
        notificationBadgeLabel.setVisible(true);
    }
    @FXML
    private void handleBellClick(MouseEvent event) {
        notificationCount = 0;
        notificationBadgeLabel.setVisible(false);

        try{
            //Load file fxml của Dialog thông báo lên
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/account/notification-dialog.fxml"));
            Parent dialogRoot = fxmlLoader.load();

            //Lấy controller của dialog và truyền danh sách thông báo sang
            NotificationDialogController dialogController = fxmlLoader.getController();
            if(notificationList.isEmpty()){
                dialogController.setNotification(List.of("There are currently no new notifications for you."));
            }
            else{
                dialogController.setNotification(notificationList);
            }

            //Biến dialog thành pop-up hiện dưới bell
            Popup popup = new Popup();
            popup.getContent().add(dialogRoot);
            popup.setAutoHide(true);//tự động đóng khi click ra ngoài vùng Dialog

            //lấy thực thể quả chuông từ sự kiện click
            ImageView clickedBell = (ImageView) event.getSource();

            //lấy tọa độ quả chuông
            Bounds bellBound = clickedBell.localToScreen(clickedBell.getBoundsInLocal());
            //cân bằng tọa độ
            double bellWidth = bellBound.getWidth();  //chiều rộng quả chuông
            double bellMaxX = bellBound.getMaxX(); //tọa độ mép phải quả chuông
            double dialogWidth = 320.0;   //chiều rộng của dialog

            double targetX = bellMaxX - dialogWidth;
            double targetY = bellBound.getMaxY() + 5;

            popup.show(clickedBell, targetX, targetY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
