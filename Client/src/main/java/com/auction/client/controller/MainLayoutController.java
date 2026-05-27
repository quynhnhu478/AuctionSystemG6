package com.auction.client.controller;

import com.auction.client.service.AppContext;
import com.auction.client.service.AppEventBus;
import com.auction.client.service.NotificationStore;
import com.auction.client.service.Session;
import com.auction.common.payload.NotificationMessage;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import com.auction.common.enums.Status;
import javafx.stage.StageStyle;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;

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
    @FXML
    private StackPane notificationBell;
    @FXML
    private Region notificationDot;
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
        initWebSocketConnection();
        AppEventBus.on("SELLER_APPROVED", (data) -> {
            Platform.runLater(() -> {
                Session.getUser().setSellerStatus("APPROVED");
                checkStatusSellerUI("APPROVED");
            });
        });
        AppEventBus.on("SELLER_REJECTED", (data) -> {
            Platform.runLater(() -> {
                Session.getUser().setSellerStatus("REJECTED");
                checkStatusSellerUI("REJECTED");
            });
        });
        AppEventBus.on("NOTIFICATION_UNREAD_CHANGED", data -> Platform.runLater(this::refreshNotificationDot));
        refreshNotificationDot();
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
        switchCenterView("/com/auction/client/fxml/auction/MyBidsView.fxml");
        updateActiveTab(myBidsButton);
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
        checkStatusSellerUI(currentUser.getSellerStatus());
        updateActiveTab(myListingsButton);
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
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/com/auction/client/fxml/auction/HomeView.fxml"
                    )
            );

            Parent liveAuctionView = loader.load();
            HomeController controller = loader.getController();
            if (controller != null) {
                controller.setCategoryFilter(categoryFilter);
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
        showLiveAuctionsView(null);
        updateCategoryTab(homeButton);
    }

    @FXML
    public void handleElectronicsCategoryClick(ActionEvent event) {
        showLiveAuctionsView("ELECTRONICS");
        updateCategoryTab(electronicsButton);
    }

    @FXML
    public void handleVehicleCategoryClick(ActionEvent event) {
        showLiveAuctionsView("VEHICLE");
        updateCategoryTab(vehicleButton);
    }

    @FXML
    public void handleArtCategoryClick(ActionEvent event) {
        showLiveAuctionsView("ART");
        updateCategoryTab(artButton);
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
                    handleUserTopicMessage(message);

                    // Nếu Server báo đã duyệt thành Seller thành công
                    if ("ROLE_UPDATED_TO_SELLER".equals(message)) {
                        // Bắn thêm Event nội bộ thông báo cho các màn hình con (nếu cần)
                        AppEventBus.emit("SELLER_APPROVED", null);
                    } else if ("REGISTRATION_REJECTED".equals(message)) {
                        AppEventBus.emit("SELLER_REJECTED", null);
                    }

                });
            }
        });
    }
    private void initWebSocketConnection() {
        String url = "ws://localhost:8080/ws-auction"; // Thay bằng URL endpoint WebSocket bên Server của bạn

        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new StringMessageConverter()); // Định dạng text/string

        // Tiến hành kết nối ngầm (Asynchronous)
        stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                System.out.println("➔ Kết nối WebSocket thành công rực rỡ!");
                stompSession = session; // Lưu lại phiên kết nối vào biến toàn cục

                // BƯỚC B: Sau khi có cổng kết nối (session) -> Bật hàm chờ lắng nghe ngay lập tức
                connectAndListenWebSocket();
            }

            @Override
            public void handleException(StompSession session, org.springframework.messaging.simp.stomp.StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                System.err.println("Lỗi Socket: " + exception.getMessage());
            }
        });
    }
    //hàm mở nút logout
    private void handleUserTopicMessage(String message) {
        if (message == null || message.isBlank() || !message.trim().startsWith("{")) {
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(message);
            NotificationMessage notification = new NotificationMessage();
            notification.setType(root.path("type").asText("INFO"));
            notification.setTitle(root.path("title").asText("Notification"));
            notification.setMessage(root.path("message").asText(""));
            if (root.has("itemId") && !root.path("itemId").isNull()) {
                notification.setItemId(root.path("itemId").asLong());
            }
            if (root.has("auctionId") && !root.path("auctionId").isNull()) {
                notification.setAuctionId(root.path("auctionId").asLong());
            }
            notification.setCreatedAt(LocalDateTime.now());
            NotificationStore.add(notification);
        } catch (Exception ignored) {
        }
    }

    private void refreshNotificationDot() {
        if (notificationDot != null) {
            notificationDot.setVisible(NotificationStore.hasUnread());
        }
    }

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

    @FXML
    private void OpenNotificationPopUp(MouseEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/account/NotificationPopup.fxml"));
            Node root = fxmlLoader.load();
            Popup popup = new Popup();
            popup.getContent().add(root);
            popup.setAutoHide(true);
            popup.show(notificationBell.getScene().getWindow(), event.getScreenX() - 285, event.getScreenY() + 18);
            refreshNotificationDot();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
