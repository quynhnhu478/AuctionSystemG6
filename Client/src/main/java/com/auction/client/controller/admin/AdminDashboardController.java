package com.auction.client.controller.admin;

import com.auction.client.service.AlertService;
import com.auction.client.service.SceneService;
import com.auction.common.payload.ItemResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.scene.input.MouseEvent;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AdminDashboardController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    @FXML private TableView<ItemResponse> tblProduct;
    private static final Logger logger = Logger.getLogger(AdminDashboardController.class.getName());
    @FXML
    private TableColumn<ItemResponse, Long> colId; // auction Id
    @FXML
    private TableColumn<ItemResponse, String> colItemName;
    @FXML
    private TableColumn<ItemResponse, LocalDateTime> colStartingTime;
    @FXML
    private TableColumn<ItemResponse, String> colSeller;
    @FXML
    private TableColumn<ItemResponse, String> colCategory;
    @FXML
    private TableColumn<ItemResponse, Double> colStartingPrice;
    @FXML
    private TableColumn<ItemResponse, LocalDateTime> colEndTime;
    @FXML
    private Button btnManageAuctions;
    @FXML
    private Button btnTerminate;
    @FXML
    private ImageView avatar;
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final ObjectMapper objectMapper = tools.jackson.databind.json.JsonMapper.builder()
            .addModule(new tools.jackson.datatype.jsr310.JavaTimeModule())
            .build();
    private ObservableList<ItemResponse> itemList = FXCollections.observableArrayList();
    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStartingPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("categories"));
        colStartingTime.setCellValueFactory(new PropertyValueFactory<>("startingTime"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        colStartingTime.setCellFactory(column -> new TableCell<ItemResponse, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });

        // Áp dụng bộ định dạng cho cột End Time
        colEndTime.setCellFactory(column -> new TableCell<ItemResponse, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });

        tblProduct.setItems(itemList);

        btnTerminate.setOnAction(event -> handleTerminateAuction());

        loadDataFromServer();

    }
    private void loadDataFromServer() {
        if (!loading.compareAndSet(false, true)){
            logger.info("Khóa màn hình loading! (loading = true) là không thể chạy");
            return;
        }

        new Thread(() -> {
            try {
                String apiUrl = "http://localhost:8080/api/admin/product_list";

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .GET()
                        .header("Accept", "application/json")
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {

                    List<ItemResponse> serverItems = objectMapper.readValue(
                            response.body(),
                            new TypeReference<List<ItemResponse>>() {}
                    );

                    Platform.runLater(() -> {
                        itemList.clear();
                        itemList.addAll(serverItems);
                        logger.info("Đã load lại bảng thành công từ server");
                        logger.info("Đổ dữ liệu lên TableView thành công!");
                        loading.set(false);
                    });
                } else {
                    logger.warning("Lỗi Server trả về mã: " + response.statusCode());
                    loading.set(false);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Không thể kết nối đến Server: " + e.getMessage(), e);
                loading.set(false);
            }
        }).start();
    }


    // Chuyển hướng sang giao diện quản lý người dùng của Admin
    @FXML
    public void switchToManageUserButton(ActionEvent event){
        SceneService.changeScene(event, "/com/auction/client/fxml/Admin/AdminManageUsers.fxml");
    }
    @FXML
    public void switchToTransactions(ActionEvent event){
        SceneService.changeScene(event, "/com/auction/client/fxml/Admin/AdminTransactions.fxml");
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

    private void handleTerminateAuction() {
        ItemResponse selectedItem = tblProduct.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            AlertService.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn một phiên đấu giá để hủy!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận hủy phiên");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn hủy (terminate) phiên đấu giá cho sản phẩm '" + selectedItem.getName() + "' không? Hành động này sẽ hoàn lại toàn bộ số tiền đóng băng cho người giữ giá cao nhất!");

        ButtonType btnYes = new ButtonType("Đồng ý", ButtonBar.ButtonData.YES);
        ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.NO);
        confirmAlert.getButtonTypes().setAll(btnYes, btnNo);

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == btnYes) {
                new Thread(() -> {
                    try {
                        String apiUrl = "http://localhost:8080/api/admin/terminate/" + selectedItem.getAuctionId();
                        HttpClient client = HttpClient.newHttpClient();
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(apiUrl))
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build();

                        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
                        if (httpResponse.statusCode() == 200) {
                            Platform.runLater(() -> {
                                AlertService.showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã hủy phiên đấu giá thành công!");
                                loadDataFromServer();
                            });
                        } else {
                            Platform.runLater(() -> {
                                AlertService.showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể hủy phiên đấu giá. Code: " + httpResponse.statusCode());
                            });
                        }
                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Lỗi khi gọi API hủy phiên: " + e.getMessage(), e);
                        Platform.runLater(() -> {
                            AlertService.showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối tới server!");
                        });
                    }
                }).start();
            }
        });
    }

    @FXML
    private void viewRequest(ActionEvent event) {
        ItemResponse selectedItem = tblProduct.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            AlertService.showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn một sản phẩm để xem chi tiết!");
            return;
        }
        logger.info("Lay dc item id " + selectedItem.getId());
        openRegistrationDialog(selectedItem.getId());
    }
    public void openRegistrationDialog(Long ItemId){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/Admin/AdminReviewProductPopUp.fxml"));

            Parent root = fxmlLoader.load();
            AdminReviewProductPopUpController controller = fxmlLoader.<AdminReviewProductPopUpController>getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Product");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            controller.initData(ItemId);
            dialogStage.showAndWait();
            logger.info("Mở Pop up review chi tiết sản phẩm thành công!");


        }catch(Exception e){
            logger.log(Level.SEVERE, "Gặp ngoại lệ khi mở hộp thoại xét duyệt đăng ký của Admin.", e);
        }

    }
}