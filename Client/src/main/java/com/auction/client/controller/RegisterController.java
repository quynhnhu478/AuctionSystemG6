package com.auction.client.controller;

import com.auction.client.config.ApiConfig;
import com.auction.client.service.SceneService;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javafx.stage.Stage;
import net.synedra.validatorfx.Validator;
import org.controlsfx.control.Notifications;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegisterController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(RegisterController.class.getName());

    @FXML
    private TextField username;
    @FXML
    private TextField useremail;
    @FXML
    private PasswordField userpassword;
    @FXML
    private PasswordField confirmpassword;
    @FXML
    private Label nameError;
    @FXML
    private Label emailError;
    @FXML
    private Label passwordError;
    @FXML
    private Label confirmError;
    @FXML
    private final Validator validator = new Validator();
    @FXML
    private Button registerButton;

    @FXML
    public void switchToLogin(ActionEvent actionEvent){
        SceneService.changeScene(actionEvent, "/com/auction/client/fxml/signin/login.fxml");
    }

    @FXML
    public void initialize(){
        validator.createCheck()
                .dependsOn("username", username.textProperty())
                .withMethod(c-> {
                    if (c.get("username") == null || c.get("username").toString().trim().isEmpty()){
                        nameError.setText("This field is required.");
                        c.error("This field is required.");
                    }
                    else{
                        nameError.setText("");
                    }
                });
        validator.createCheck()
                .dependsOn("useremail", useremail.textProperty())
                .withMethod( c->{
                    if( c.get("useremail") ==null || c.get("useremail").toString().trim().isEmpty()){
                        emailError.setText("This field is required.");
                        c.error("This field is required.");
                    }
                    else{
                        emailError.setText("");
                    }
                });
        validator.createCheck()
                .dependsOn("userpassword", userpassword.textProperty())
                .dependsOn("confirmpassword", confirmpassword.textProperty())
                .withMethod(c->{
                    String pass = c.get("userpassword") == null? "" : c.get("userpassword").toString().trim();
                    String confirm = c.get("confirmpassword") == null? "" : c.get("confirmpassword").toString().trim();
                    if (pass.isEmpty()){
                        passwordError.setText("This field is required.");
                        confirmError.setText("");
                        c.error("This field is required.");
                    }
                    else if (!confirm.equals(pass)){
                        confirmError.setText("Password does not match!");
                        passwordError.setText("");
                        c.error("Password does not match");
                    }
                    else{
                        passwordError.setText("");
                        confirmError.setText("");
                    }
                });
    }

    @FXML
    void RegisterButton(ActionEvent event){
        if (!validator.validate()){
            logger.warning("Dữ liệu đăng ký không hợp lệ thông qua kiểm tra của Validator.");
            Notifications.create()
                    .title("Error")
                    .text("Please fill all the blankets")
                    .showError();

        }
        else{
            try{
                String name = username.getText() == null ? "" : username.getText().trim();
                String email = useremail.getText() == null ? "" : useremail.getText().trim();
                String password = userpassword.getText() == null ? "" : userpassword.getText();

                String json = String.format(
                        "{ \"name\": \"%s\", \"email\": \"%s\", \"password\": \"%s\"}",
                        escapeJson(name), escapeJson(email), escapeJson(password)
                );
                logger.info("JSON gửi đi: " + json);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(ApiConfig.BASE_URL + "/api/auth/register"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                new Thread(() -> {
                    try{
                        HttpResponse<String> response = client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());
                        Platform.runLater(() -> {
                            try{
                                logger.info("Status code: " + response.statusCode());
                                logger.info("Response body: " + response.body());
                                if (response.statusCode() >= 200 && response.statusCode() < 300 ){
                                    Notifications.create()
                                            .title("Success")
                                            .text("Register successful!")
                                            .showInformation();
                                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/signin/login.fxml"));
                                    Parent root = loader.load();

                                    Stage stage = (Stage) registerButton.getScene().getWindow();
                                    stage.setScene(new Scene(root));
                                }
                                else {
                                    Notifications.create()
                                            .title("Error")
                                            .text(response.body() == null || response.body().isBlank() ? "Register failed!" : response.body())
                                            .showError();
                                }
                            }
                            catch (Exception e){
                                logger.log(Level.SEVERE, "Gặp ngoại lệ xử lý tải giao diện sau khi nhận phản hồi đăng ký từ server.", e);
                            }

                        });
                    }
                    catch (Exception e){
                        logger.log(Level.SEVERE, "Lỗi kết nối mạng trong quá trình gửi luồng đăng ký tài khoản bất đồng bộ.", e);
                        Platform.runLater(() -> Notifications.create()
                                .title("Error")
                                .text("Cannot connect to server.")
                                .showError());
                    }
                }).start();

            }
            catch(Exception e){
                logger.log(Level.SEVERE, "Gặp ngoại lệ khi đóng gói JSON và khởi tạo tiến trình đăng ký.", e);
            }
        }
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}
