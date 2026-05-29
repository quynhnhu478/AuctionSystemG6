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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import javafx.stage.Popup;
import javafx.stage.Stage;
import com.auction.common.enums.Status;

import org.springframework.messaging.simp.stomp.StompSession;


import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.auction.client.controller.auction.MyBidsController;
import com.auction.client.controller.seller.ItemContainerController;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;

public class MainLayoutController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(MainLayoutController.class.getName());

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
    private Region notificationDot;

    @FXML private Label lblBellBadge; // Khai báo ánh xạ đến Label số đỏ fxml
    private int unreadNotificationsCount = 0;
    private NotificationPopupController currentPopupController; // Lưu giữ reference để đẩy tin real-time vào trực tiếp
    private final ObjectMapper mapper = new JsonMapper().builder().addModule(new JavaTimeModule()).build();

    // Theo dõi trạng thái nộp hồ sơ của người bán (seller registration)
    private static boolean sellerApplicationSubmitted = false;
    private static MainLayoutController instance;
    private StompSession stompSession;
    private Popup notificationPopup;
    private String currentMainTab = "LIVE";
    private String currentCategoryFilter = null;
    private Parent cachedMyBidsView;
    private MyBidsController cachedMyBidsController;
    private Parent cachedMyListingsView;
    private ItemContainerController cachedMyListingsController;
    private Parent cachedLiveAuctionView;
    private HomeController cachedHomeController;

    @FXML
    private void initialize() {
        UserResponse user = Session.getUser();

        if (user != null) {
            userNameField.setText(user.getName());
            AppContext.getInstance().setUserId(user.getId());
        }
        instance = this;

        // Thêm MainLayoutController vào AppContext để đổi trang ở các Controller khác dễ dàng
        AppContext.getInstance().setMainLayoutController(this);
        WebsocketConfigService.getInstance().connect();

        AppEventBus.on("SELLER_APPROVED", (data) ->{
            Platform.runLater(() -> {
                Session.getUser().setSellerStatus("APPROVED");
                logger.info("Chuyển màn hình giao diện cho người dùng được đồng ý duyệt quyền seller.");
                checkStatusSellerUI("APPROVED");
            });
        });

        AppEventBus.on("SELLER_REJECTED", (data) ->{
            Platform.runLater(() -> {
                Session.getUser().setSellerStatus("REJECTED");
                logger.info("Chuyển màn hình giao diện cho người dùng bị từ chối duyệt quyền seller.");
                checkStatusSellerUI("REJECTED");
            });
        });

        AppEventBus.on("NOTIFICATION_UNREAD_CHANGED", (data) -> {
            Platform.runLater(() -> notificationDot.setVisible(Boolean.TRUE.equals(data)));
        });

        AppEventBus.on("NEW_NOTIFICATION_RECEIVED", (payload) -> {
                    try {
                        // Khấu tách chuỗi JSON nhận được thành đối tượng JsonNode của Jackson
                        tools.jackson.databind.JsonNode notiNode = mapper.readTree((String) payload);

                        Platform.runLater(() -> {
                            // Tăng số lượng thông báo chưa đọc
                            unreadNotificationsCount++;

                            // Hiện chấm đỏ lên (hoặc nếu bạn đã đổi sang Label số thì set Text tại đây)
                            notificationDot.setVisible(true);

                            // Nếu người dùng ĐANG mở xem popup chuông, nạp nóng dòng này trực tiếp vào màn hình luôn
                            if (notificationPopup != null && notificationPopup.isShowing() && currentPopupController != null) {
                                currentPopupController.addNotificationRow(notiNode, true);
                            }
                        });
                    }catch (Exception e) {
                        logger.log(Level.SEVERE, "Lỗi phân tích cú pháp thông báo WebSocket tại MainLayout", e);
                    }
                });
        Platform.runLater(this::openDefaultCenterView);
    }

    private void openDefaultCenterView() {
        UserResponse user = Session.getUser();
        if (user != null && user.getRoles() != null && user.getRoles().contains("SELLER")) {
            openMyListingsView(currentCategoryFilter);
        } else {
            showLiveAuctionsView();
        }
    }

    // Hàm để thay đổi vùng hiển thị Trung tâm (Center) bằng mã nguồn Java
    public void setCenterView(Node node){
        contentPane.setCenter(node);
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    // Hàm dùng chung để đổi màu tab đang hoạt động thành vàng và các tab khác thành trắng
    private void updateActiveTab(Button activeButton) {
        // Tạo style chẩn cho các tab bình thường (Màu trắng)
        String normalStyle = "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0; -fx-padding: 0 20 0 20;";
        // Style dành riêng cho tab đang được chọn (Màu vàng #dfb160)
        String activeStyle = "-fx-background-color: transparent; -fx-text-fill: #dfb160; -fx-font-size: 14; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0; -fx-padding: 0 20 0 20;";

        // Đặt lại style mặc định cho toàn bộ nút menu điều hướng chính
        liveAuctionsButton.setStyle(normalStyle);
        myBidsButton.setStyle(normalStyle);
        myListingsButton.setStyle(normalStyle);

        // Kích hoạt màu vàng nổi bật cho nút vừa tương tác nhấn chuột
        activeButton.setStyle(activeStyle);
    }

    // Hàm phụ trợ hỗ trợ nạp tệp và hoán đổi Khung nhìn hiển thị ở vùng Center từ bất cứ đâu
    public static void switchCenterView(String fxmlPath) {
        try {
            if (instance != null) {
                FXMLLoader loader = new FXMLLoader(MainLayoutController.class.getResource(fxmlPath));
                Parent view = loader.load();
                instance.contentPane.setCenter(view); // Thay thế vùng nội dung trung tâm hiện tại
            }
            else{
                logger.warning("Lỗi: Thực thể MainLayoutController instance hiện tại đang bị null!");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Gặp sự cố IO khi hoán đổi khung nhìn trung tâm tại đường dẫn FXML: " + fxmlPath, e);
        }
    }


    private boolean hasAuctions() {
        // TODO: Kiểm tra danh sách sản phẩm thực tế từ cơ sở dữ liệu.
        // Hiện tại tạm thời chưa có sản phẩm nào nên mặc định trả về false.
        return false;
    }

    @FXML
    private void handleMyBidsLayout(ActionEvent event) {
        currentMainTab = "MY_BIDS";
        openMyBidsView(currentCategoryFilter);
    }

    @FXML
    private void handleMyListingsLayout(ActionEvent event) {
        currentMainTab = "MY_LISTINGS";
        openMyListingsView(currentCategoryFilter);
    }

    private void openMyListingsView() {
        openMyListingsView(currentCategoryFilter);
    }

    private void openMyListingsView(String categoryFilter) {
        UserResponse currentUser = Session.getUser();
        if (currentUser == null) {
            Label placeholder = new Label("Session expired. Please login again.");
            placeholder.setStyle("-fx-font-size: 16px; -fx-text-fill: #523c34;");
            contentPane.setCenter(placeholder);
            return;
        }

        checkStatusSellerUI(currentUser.getSellerStatus(), categoryFilter);
        updateActiveTab(myListingsButton);
    }

    private void checkStatusSellerUI(String status){
        checkStatusSellerUI(status, currentCategoryFilter);
    }

    private void checkStatusSellerUI(String status, String categoryFilter){
        if (status == null){
            switchCenterView("/com/auction/client/fxml/seller/become-seller-view.fxml");
        }
        else if(status.equalsIgnoreCase(Status.PENDING.toString())){
            switchCenterView("/com/auction/client/fxml/seller/my-listings-under-review.fxml");
        }
        else if(status.equalsIgnoreCase(Status.APPROVED.toString())){
            showApprovedMyListingsView(categoryFilter);
        }
        else if(status.equalsIgnoreCase(Status.REJECTED.toString())){
            switchCenterView("/com/auction/client/fxml/seller/become-seller-view.fxml");
        }
    }

    private void showApprovedMyListingsView(String categoryFilter) {
        try {
            if (cachedMyListingsView == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/auction/client/fxml/seller/my-listings-view.fxml")
                );
                cachedMyListingsView = loader.load();
                cachedMyListingsController = loader.getController();
            }

            if (cachedMyListingsController != null) {
                cachedMyListingsController.setCategoryFilter(categoryFilter);
            }

            setCenterView(cachedMyListingsView);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot load My Listings view", e);
        }
    }

    public static void setSellerApplicationSubmitted(boolean submitted) {
        sellerApplicationSubmitted = submitted;
    }

    @FXML
    // Hiển thị trang danh sách sản phẩm đấu giá trực tiếp Live Auctions
    public void showLiveAuctionsView() {
        showLiveAuctionsView(null);
    }

    public void showLiveAuctionsView(String categoryFilter) {
        try {
            if (cachedLiveAuctionView == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource(
                                "/com/auction/client/fxml/auction/HomeView.fxml"
                        )
                );

                cachedLiveAuctionView = loader.load();
                cachedHomeController = loader.getController();
            }

            if (cachedHomeController != null) {
                cachedHomeController.setCategoryFilter(categoryFilter);
            }

            // Thay đổi phân vùng hiển thị trung tâm nội dung
            setCenterView(cachedLiveAuctionView);

            // Cập nhật lại trạng thái màu sắc thanh tab menu
            updateActiveTab(liveAuctionsButton);
            updateCategoryTabByFilter(categoryFilter);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Gặp ngoại lệ khi xử lý tải và kết xuất HomeView cho luồng Live Auctions.", e);
        }
    }
    private void applyCategoryFilter(String category) {
        currentCategoryFilter = category;

        if ("LIVE".equals(currentMainTab)) {
            showLiveAuctionsView(category);
        } else if ("MY_BIDS".equals(currentMainTab)) {
            openMyBidsView(category);
        } else if ("MY_LISTINGS".equals(currentMainTab)) {
            openMyListingsView(category);
        }

        updateCategoryTabByFilter(category);
    }
    private void openMyBidsView(String categoryFilter) {
        try {
            if (cachedMyBidsView == null) {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/auction/client/fxml/auction/MyBidsView.fxml")
                );
                cachedMyBidsView = loader.load();
                cachedMyBidsController = loader.getController();
            }

            if (cachedMyBidsController != null) {
                cachedMyBidsController.setCategoryFilter(categoryFilter);
            }

            setCenterView(cachedMyBidsView);
            updateActiveTab(myBidsButton);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot load My Bids view", e);
        }
    }
    @FXML
    public void handleLiveAuctionsLayout(ActionEvent event) {
        currentMainTab = "LIVE";
        showLiveAuctionsView(currentCategoryFilter);
    }

    @FXML
    public void handleHomeCategoryClick(ActionEvent event) {
        applyCategoryFilter(null);
    }

    @FXML
    public void handleElectronicsCategoryClick(ActionEvent event) {
        applyCategoryFilter("ELECTRONICS");
    }

    @FXML
    public void handleVehicleCategoryClick(ActionEvent event) {
        applyCategoryFilter("VEHICLE");
    }

    @FXML
    public void handleArtCategoryClick(ActionEvent event) {
        applyCategoryFilter("ART");
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

    // Hàm xử lý kích hoạt hiển thị cửa sổ nhỏ (Popup) quản lý tài khoản/đăng xuất
    @FXML
    private void OpenAccountPopUp(MouseEvent event){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/account/AccountPopup.fxml"));
            Node root = fxmlLoader.load();
            AccountPopupController controller = fxmlLoader.getController();
            Stage mainStage = (Stage) myBidsButton.getScene().getWindow();
            controller.setMainStage(mainStage);
            controller.setUserBalanceInput(Session.getUser().getTotalBalance());
            Popup popup = new Popup();
            popup.getContent().add(root);
            popup.setAutoHide(true);
            ImageView avatar = (ImageView) event.getSource();
            double x = event.getScreenX() - 110;
            double y = event.getScreenY() + 20;
            popup.show(avatar.getScene().getWindow(), x, y);

        }
        catch(Exception e){
            logger.log(Level.SEVERE, "Gặp ngoại lệ khi khởi tạo hoặc định vị Popup tài khoản cá nhân.", e);
        }
    }

    // Hàm xử lý hiển thị cửa sổ Popup xem danh sách các thông báo hệ thống
    @FXML
    private void OpenNotificationPopUp(MouseEvent event) {
        try {
            if (notificationPopup != null && notificationPopup.isShowing()) {
                notificationPopup.hide();
                return;
            }

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/account/NotificationPopup.fxml"));
            Node root = fxmlLoader.load();

            // Lấy và lưu trữ reference của Controller thuộc Popup để dùng cho việc đẩy tin real-time
            currentPopupController = fxmlLoader.getController();

            notificationPopup = new Popup();
            notificationPopup.getContent().add(root);
            notificationPopup.setAutoHide(true);
            notificationPopup.setOnHidden(e -> {
                notificationPopup = null;
                currentPopupController = null; // Giải phóng bộ nhớ khi tắt popup
            } );

            Node source = (Node) event.getSource();
            double x = event.getScreenX() - 300;
            double y = event.getScreenY() + 18;
            notificationPopup.show(source.getScene().getWindow(), x, y);

            // KHI NGƯỜI DÙNG ĐÃ BẤM VÀO XEM CHUÔNG -> Ẩn số đỏ thông báo đi
            unreadNotificationsCount = 0;
            lblBellBadge.setVisible(false);
        }
        catch(Exception e){
            logger.log(Level.SEVERE, "Gặp ngoại lệ trong luồng khởi tạo và hiển thị Popup thông báo.", e);
        }
    }


}
