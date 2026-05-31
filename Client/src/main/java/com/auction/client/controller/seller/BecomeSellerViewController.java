package com.auction.client.controller.seller;

import com.auction.client.config.ApiConfig;
import com.auction.client.controller.MainLayoutController;
import com.auction.client.service.AppContext;
import com.auction.client.service.Session;
import com.auction.common.payload.SellerRegistrationRequest;

import com.auction.common.payload.UserResponse;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BecomeSellerViewController {
    private static final Logger logger = Logger.getLogger(BecomeSellerViewController.class.getName());

    @FXML
    private void openRegisterDialog(ActionEvent event)  {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/seller-registration-view.fxml"));
            Parent root = fxmlLoader.load();
            SellerRegistrationViewController controller = fxmlLoader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Register as a Seller");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            if (controller.isAllStepFinished()) {
                SellerRegistrationRequest registrationRequest = controller.getCompletedRequest();
                sendRegistrationRequest(registrationRequest);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception occurred while initializing seller registration view stage popup", e);
        }
    }

    private void sendRegistrationRequest(SellerRegistrationRequest registrationRequest){
        UserResponse currentUser = Session.getUser();
        if (currentUser == null) {
            showNotification(Alert.AlertType.ERROR, "Error", "Session expired. Please log in again.");
            logger.warning("Error: Session expired. Please log in again.");
            return;
        }

        Long currentUserId = currentUser.getId();

        String name = registrationRequest.getName();
        String email = registrationRequest.getEmail();
        String identityNumber = registrationRequest.getIdentityNumber();
        String phoneNumber = registrationRequest.getPhoneNumber();
        String address = registrationRequest.getAddress();
        String identifiedImageFront =  registrationRequest.getIdentifiedImageFront();
        String identifiedImageBehind = registrationRequest.getIdentifiedImageBehind();
        String json = String.format(
                "{ \"name\": \"%s\", \"identityNumber\": \"%s\", \"phoneNumber\": \"%s\",\"email\": \"%s\", \"address\": \"%s\",\"identifiedImageFront\": \"%s\",\"identifiedImageBehind\": \"%s\"}",
                name, identityNumber, phoneNumber, email, address, identifiedImageFront, identifiedImageBehind
        );

        Task<HttpResponse<String>> task = new Task<>(){
            @Override
            protected HttpResponse<String> call() throws Exception {
                HttpClient client = HttpClient.newHttpClient();

                String serverApiUrl = ApiConfig.BASE_URL + "/api/seller/register";
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(serverApiUrl))
                        .header("Content-Type", "application/json")
                        .header("userId", String.valueOf(currentUserId))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                return client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            }
        };

        task.setOnSucceeded(event -> {
            HttpResponse<String> response = task.getValue();
            handleApiServer(response);
        });

        task.setOnFailed(event -> {
            Throwable e = task.getException();
            logger.log(Level.SEVERE, "Network pipeline worker transaction processing crash", e);
            showNotification(Alert.AlertType.ERROR, "Connection Error", "Error: Could not transmit payload metadata securely to Server endpoint.");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // Khối quy trình logic đánh giá mã lỗi phản hồi của máy chủ
    private void handleApiServer(HttpResponse<String> response){
        logger.log(Level.INFO, "Server diagnostic evaluation transaction code: {0}", response.statusCode());
        logger.log(Level.INFO, "Registration dispatch transaction server telemetry stream back response body: {0}", response.body());

        if (response.statusCode() == 200) {
            if (Session.getUser() != null) {
                Session.getUser().setSellerStatus("PENDING");
            }
            showNotificationSuccess();
        }
        else if (response.statusCode() >= 400 && response.statusCode() < 500) {
            String serverWarningMessage = response.body();
            showNotification(
                    Alert.AlertType.WARNING,
                    "You have already sent a registration!",
                    serverWarningMessage
            );
        } else {
            showNotification(Alert.AlertType.ERROR, "Registration Error", "Internal system processing anomaly detected.");
        }
    }

    // Khối thường trình trình tải bộ điều khiển bật lên thông báo đăng ký thành công
    private void showNotificationSuccess(){
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/application-submitted-dialog.fxml"));
            Parent root = fxmlLoader.load();
            ApplicationSubmittedDialogController controller = fxmlLoader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Announcement");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            if (controller.getHasclosed()) {
                Platform.runLater(() -> {
                    // Extract MainLayout from AppContext and invoke view loader transition routine
                    MainLayoutController mainLayout = AppContext.getInstance().getMainLayoutController();
                    if (mainLayout != null) {
                        MainLayoutController.switchCenterView("/com/auction/client/fxml/seller/my-listings-under-review.fxml");
                    }
                });
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error: Cannot upload and execute success configuration confirmation overlay element payload: " + e.getMessage(), e);
        }
    }

    private void showNotification(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
