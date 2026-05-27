package com.auction.client.controller.admin;

import com.auction.client.service.AlertService;
import com.auction.client.service.Session;
import com.auction.common.payload.HandleSellerRegistrationRequest;
import com.auction.common.payload.SellerRegistrationResponse;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

public class ReviewSellerRequestController {

    //Khai báo các fx:id đồng bộ chính xác với file FXML
    @FXML
    private TextField sellerNameField;

    @FXML
    private TextField identityField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField addressField;

    @FXML
    private ImageView frontImageView;

    @FXML
    private ImageView backImageView;

    @FXML
    private Button approveButton;

    @FXML
    private Button rejectButton;

    // Biến lưu trữ đối tượng Request hiện tại
    private Object currentRequest;

    private Long currentUserId;
    private boolean isSuccess = false;
    public boolean isSuccess() { return this.isSuccess; }
    private static final String BASE_URL = "http://localhost:8080";
    public void initData(Long userId) {
        this.currentUserId = userId;

        // Tạo Task chạy ngầm để kéo dữ liệu chi tiết từ AdminController (Backend)
        Task<SellerRegistrationResponse> task = new Task<>() {
            @Override
            protected SellerRegistrationResponse call() throws Exception {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/admin/seller-registration1/" + userId))
                        .GET()
                        .header("Accept", "application/json")
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("74: "+response.statusCode());
                System.out.println(response.body());
                if (response.statusCode() == 200) {
                    try {
                        // Khởi tạo mapper qua Builder của Jackson 3
                        ObjectMapper mapper = JsonMapper.builder()
                                .addModule(new JavaTimeModule()) // Đúng package tools.jackson
                                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // Tránh lỗi lệch trường
                                .build();

                        return mapper.readValue(response.body(), SellerRegistrationResponse.class);

                    } catch (Exception e) {
                        System.out.println("LỖI PARSE JSON:");
                        e.printStackTrace();
                        throw e;
                    }
                } else {
                    throw new RuntimeException("Failed to load registration details");
                }
            }
        };

        // Khi lấy được dữ liệu về, đổ lên các ô Input/Label và ImageView trên UI
        task.setOnSucceeded(e -> {
            SellerRegistrationResponse data = task.getValue();

            System.out.println("Data nhan duoc o UI: " + data);
            System.out.println("Name: " + data.getName());

            sellerNameField.setText(data.getName());
            identityField.setText(data.getIdentityNumber());
            phoneField.setText(data.getPhoneNumber());
            emailField.setText(data.getEmail());
            addressField.setText(data.getAddress());

            if (data.getIdentifiedImageFront() != null) {
                String frontImageUrl = BASE_URL + data.getIdentifiedImageFront();

                frontImageView.setImage(new Image(frontImageUrl, true));
            }

            if (data.getIdentifiedImageBehind() != null) {
                String backImageUrl = BASE_URL + data.getIdentifiedImageBehind();
                backImageView.setImage(new Image(backImageUrl, true));
            }
        });
        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            System.out.println("Loi chay ngam!");
            if (exception != null) {
                exception.printStackTrace();
            }
        });

        new Thread(task).start();
    }


    // Xử lý sự kiện khi Admin bấm nút Phê duyệt [Approve]
    @FXML
    void handleApprove(ActionEvent event) {
        System.out.println("Admin đã bấm PHÊ DUYỆT yêu cầu nâng cấp Seller!");
        HandleSellerRegistrationRequest handleSellerRegistrationRequest = new HandleSellerRegistrationRequest();
        handleSellerRegistrationRequest.setRegistrationId(currentUserId);
        handleSellerRegistrationRequest.setAdminAction("APPROVE");
        sendAdminAction(handleSellerRegistrationRequest);
    }

    //Xử lý sự kiện khi Admin bấm nút Từ chối [Reject]
    @FXML
    void handleReject(ActionEvent event) {
        System.out.println("Admin clicked REJECT the Seller upgrade request!");
        HandleSellerRegistrationRequest handleSellerRegistrationRequest = new HandleSellerRegistrationRequest();
        handleSellerRegistrationRequest.setRegistrationId(currentUserId);
        handleSellerRegistrationRequest.setAdminAction("REJECT");
        sendAdminAction(handleSellerRegistrationRequest);
    }

    @FXML
    void handleCancel(ActionEvent event) { //
        closeWindow();
    }

    //Hàm bổ trợ dùng chung để đóng nhanh Stage (Cửa sổ) hiện tại
    private void closeWindow() {
        Stage stage = (Stage) sellerNameField.getScene().getWindow();
        stage.close();
    }
    private void sendAdminAction(HandleSellerRegistrationRequest handleSellerRegistrationRequest) {
        Long registrationId = handleSellerRegistrationRequest.getRegistrationId();
        String adminAction = handleSellerRegistrationRequest.getAdminAction();
        String jsonBody = String.format(
                "{\"registrationId\": %d, \"adminAction\": \"%s\"}",
                registrationId, adminAction
        );

        Task<HttpResponse<String>> task = new Task<>() {
            @Override
            protected HttpResponse<String> call() throws Exception {
                HttpClient client = HttpClient.newHttpClient();


                Set<String> adminRole = Session.getUser().getRoles();
                if (!adminRole.contains("ADMIN")) {
                    throw new IllegalAccessException ("Admin required!");
                }
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/admin/handle_sellerRegistration"))
                        .header("Content-Type", "application/json")
                        .header("X-Role", "ADMIN")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                return client.send(request, HttpResponse.BodyHandlers.ofString());
            }
        };

        task.setOnSucceeded(e -> {
            HttpResponse<String> response = task.getValue();
            if (response.statusCode() == 200) {
                isSuccess = true;
                closeWindow();
            } else if (response.statusCode() == 400) {
                AlertService.showAlert(Alert.AlertType.ERROR, "Error", "Cannot Handle!");
                System.out.println(response.body());
            }
        });

        task.setOnFailed(e -> {
            if (task.getException()!=null){
                task.getException().printStackTrace();
            }
            isSuccess = false;
            AlertService.showAlert(Alert.AlertType.ERROR, "Error connect", "Cannot send request!");
        });

        new Thread(task).start();
    }

}