package com.auction.client.controller;

import com.auction.client.service.AlertService;
import com.auction.client.service.AppContext;
import com.auction.client.service.Session;
import com.auction.client.service.WebsocketConfigService;
import com.auction.common.payload.UserResponse;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.stage.Stage;
import org.springframework.messaging.simp.stomp.StompSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountPopupController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(AccountPopupController.class.getName());

    private StompSession stompSession;
    private Stage mainStage;
    @FXML
    private Button logoutButton;
    @FXML
    private TextField balanceInput;


    public void initialize(){
        balanceInput.setEditable(false);
    }


    public void setMainStage(Stage mainStage) {
        this.mainStage = mainStage;
    }

    @FXML
    public void setUserBalanceInput(double balanceFromServer){
        DecimalFormat moneyFormat = new DecimalFormat("#,##0.##");
        String formattedBalance = moneyFormat.format(balanceFromServer);
        balanceInput.setText(formattedBalance);
    }

    @FXML
    private void handleEditBalanceClick(MouseEvent event){
        balanceInput.setDisable(false);
        balanceInput.setEditable(true);
        Platform.runLater(()->{
            balanceInput.requestFocus();
            balanceInput.selectAll();
        });
    }

    @FXML
    public void handleBalanceEnter(ActionEvent event){
        String inputBalance = balanceInput.getText().replace(",", "").trim();
        if  (inputBalance.isEmpty()){
            return;
        }
        try {
            double amount = Double.parseDouble(inputBalance);
            if (amount < 0) {
                throw new NumberFormatException();
            }
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setContentText("Do you confirm your balance?");
            if (alert.showAndWait().get() == ButtonType.OK) {
                Long userId = Session.getUser().getId();
                Task<Integer> task = new Task<>() {
                    @Override
                    protected Integer call() throws Exception {
                        HttpClient httpClient = HttpClient.newHttpClient();
                        String urlPath = String.format(
                                "http://localhost:8080/api/auth/update_balance?userId=%d&balance=%f",
                                userId, amount
                        );
                        HttpRequest httpRequest = HttpRequest.newBuilder()
                                .uri(URI.create(urlPath))
                                .header("Content-Type", "application/json")
                                .PUT(HttpRequest.BodyPublishers.noBody())
                                .build();
                        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                        return response.statusCode();
                    }
                };
                task.setOnSucceeded(e -> {
                    int statusCode = task.getValue();
                    if (statusCode == 200) {
                        Session.getUser().setBalance(amount);
                        DecimalFormat moneyFormat = new DecimalFormat("#,##0.##");
                        balanceInput.setText(moneyFormat.format(amount));
                        balanceInput.setEditable(false);
                        logger.info("Balance updated successfully!");
                    } else {
                        logger.warning("Balance update failed! Status code: " + statusCode);
                    }
                });
                task.setOnFailed(e -> {
                    Throwable error = task.getException();
                    logger.log(Level.SEVERE, "Gặp lỗi khi cập nhật số dư người dùng trên Server: " + error.getMessage(), error);
                });
                Thread thread = new Thread(task);
                thread.start();
            }

        }catch (NumberFormatException e){
            AlertService.showAlert(Alert.AlertType.ERROR,"Error", "Please enter a positive number!");
        }
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
            WebsocketConfigService.getInstance().disconnect();
            logger.info("Ngắt kết nối cho userId " + Session.getUser().getId());
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Gặp lỗi khi ngắt kết nối WebSocket trong quá trình đăng xuất.", e);
        }
        Long userId = Session.getUser().getId();
        logger.info("Thoat login cho user " + userId);
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
            logger.info("Server response received");
            Session.setUser(null);
            closeMainLayOut();
        });
        task.setOnFailed(event -> {
            logger.warning("Gửi request đăng xuất lên server thất bại.");
            AlertService.showAlert(Alert.AlertType.ERROR, "Login Failed", "Login Failed");
        });
        Thread thread = new Thread(task);
        thread.start();
    }

    private void closeMainLayOut(){
        try{
            logger.info("Bat dau dong trang");
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
                logger.info("Dong lop Main Layout thanh cong");
            }
        }catch(Exception e){
            logger.log(Level.SEVERE, "Gặp ngoại lệ khi chuyển hướng giao diện và đóng Main Layout.", e);
        }
    }

    public void setStompSession(StompSession session) {
        this.stompSession = session;
    }
}