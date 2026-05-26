package com.auction.client.controller.admin;

import com.auction.client.service.AlertService;
import com.auction.client.service.Session;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.springframework.messaging.simp.stomp.StompSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AdminLogoutController {
    private Stage mainStage;
    private StompSession stompSession;
    @FXML
    private Button logoutButton;
    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
    }
    @FXML
    private void Logout(ActionEvent event){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setContentText("Are you sure you want to logout?");
        if (alert.showAndWait().get() != ButtonType.OK){
            return;
        }
        try {
            if (stompSession != null && stompSession.isConnected()) {
                stompSession.disconnect();
                System.out.println("Socket disconnected!");
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        Long userId = Session.getUser().getId();
        System.out.println("Thoat login cho user "+userId);
        sendApiToServer(userId);
    }
    private void sendApiToServer(Long userId){
        Task<HttpResponse<String>> task = new Task<>(){
            @Override
            protected HttpResponse<String> call() throws Exception {
                HttpClient httpClient = HttpClient.newHttpClient();
                String url = "http://localhost:8080/api/auth/logout?userId="+userId;
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            }
        };
        task.setOnSucceeded(e -> {
            System.out.println("Server response received");
            Session.setUser(null);
            closeMainLayOut();

        });
        task.setOnFailed(event -> {
            AlertService.showAlert(Alert.AlertType.ERROR, "Login Failed", "Login Failed");

        });
        Thread thread = new Thread(task);
        thread.start();
    }

    private void closeMainLayOut(){
        try{
            System.out.println("Bat dau dong trang");
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/signin/login.fxml"));
            Parent root = fxmlLoader.load();
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            loginStage.setTitle("Login");
            loginStage.show();

            Popup popupStage = (Popup) logoutButton.getScene().getWindow() ;
            popupStage.hide();

            if (this.mainStage!= null) {
                this.mainStage.close();
                System.out.println("Dong lop Main Layout thanh cong");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void setStompSession(StompSession session) {
        this.stompSession = session;
    }
}
