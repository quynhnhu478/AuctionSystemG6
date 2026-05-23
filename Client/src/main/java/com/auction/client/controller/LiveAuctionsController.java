package com.auction.client.controller;

import com.auction.client.controller.seller.BecomeASellerController;
import com.auction.client.service.Session;
import com.auction.common.payload.SellerRegistrationRequest;

import com.auction.common.payload.UserResponse;
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

public class LiveAuctionsController {
    @FXML
    private void openRegisterDialog(ActionEvent event)  {
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/become-seller.fxml"));
            Parent root = fxmlLoader.load();
            BecomeASellerController controller = fxmlLoader.getController();

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
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    private void sendRegistrationRequest(SellerRegistrationRequest registrationRequest){
        UserResponse currentUser = Session.getUser();
        if(currentUser==null){
            showNotification(Alert.AlertType.ERROR, "Error", "Session expired. Please log in again.");
            System.out.println("Error: Session expired. Please log in again.");
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
                name,identityNumber,phoneNumber,email,address,identifiedImageFront,identifiedImageBehind
        );
        Task<HttpResponse<String>> task = new Task<>(){
            @Override
            protected HttpResponse<String> call() throws Exception{
                HttpClient client = HttpClient.newHttpClient();

                String serverApiUrl = "http://localhost:8080/api/seller/register";
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
            e.printStackTrace();
            showNotification(Alert.AlertType.ERROR, "Error connect", "Error: could not send to Server");
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();


    }
    // hàm phân tích mã lỗi khi server gửi về
    private void handleApiServer(HttpResponse<String> response){
        System.out.println(response.statusCode());
        if (response.statusCode() == 200){
            showNotificationSuccess();
        }
        else if (response.statusCode() >= 400 && response.statusCode() < 500) {
            String serverWarningMessage = response.body();
            showNotification(
                    Alert.AlertType.WARNING,
                    "You have already send a registration!",
                    serverWarningMessage
            );
        }else{

            showNotification(Alert.AlertType.ERROR, "Error Register", "Error system");
        }

    }
    // hàm mở popup thông báo đăng ký thành công.
    private void showNotificationSuccess(){
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/application-submitted-dialog.fxml"));
            Parent root = fxmlLoader.load();
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Announcement");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        } catch (Exception e){
            System.out.println("Error: cannot upload success notification!" +e.getMessage());
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
