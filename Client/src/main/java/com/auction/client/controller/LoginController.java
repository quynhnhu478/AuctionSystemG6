package com.auction.client.controller;

import com.auction.client.service.AppContext;
import com.auction.client.service.Session;

import com.auction.common.payload.UserResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Window;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import net.synedra.validatorfx.Validator;
import org.controlsfx.control.Notifications;
import tools.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;


public class LoginController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(LoginController.class.getName());

    @FXML
    private TextField username;
    @FXML
    private PasswordField userpassword;
    @FXML
    private Label nameError;
    @FXML
    private Label passwordError;
    @FXML
    private final Validator validator = new Validator();
    @FXML
    private Button loginButton;

    @FXML
    public void switchToRegister(ActionEvent actionEvent) {
        try {
            // Tải trực tiếp file register.fxml
            Parent registerRoot = FXMLLoader.load(getClass().getResource("/com/auction/client/fxml/signin/register.fxml"));

            // Thay thế root của Scene hiện tại
            javafx.scene.Node source = (javafx.scene.Node) actionEvent.getSource();
            source.getScene().setRoot(registerRoot);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Không thể chuyển sang trang Register!", e);
        }
    }

    @FXML
    public void initialize(){
        validator.createCheck()
                .dependsOn("username", username.textProperty())
                .withMethod(c->{
                    if (c.get("username") == null || c.get("username").toString().trim().isEmpty()){
                        nameError.setText("This field is required.");
                        c.error("This field is required");
                    }
                    else{
                        nameError.setText("");
                    }
                });
        validator.createCheck()
                .dependsOn("userpassword", userpassword.textProperty())
                .withMethod(c->{
                    String pass = c.get("userpassword") == null? "" : c.get("userpassword").toString().trim();
                    if (pass.isEmpty()){
                        passwordError.setText("This field is required.");
                        c.error("This field is required.");
                    }else{
                        passwordError.setText("");
                    }
                });
    }

    @FXML
    void LoginButton(ActionEvent event){
        if(!validator.validate()){
            Notifications.create()
                    .title("Error")
                    .text("Please fill all the blanks")
                    .showError();
        }
        else{
            try{
                final Stage ownerStage = resolveStageFromEvent(event);
                String name = username.getText() == null ? "" : username.getText().trim();
                String pass = userpassword.getText() == null ? "" : userpassword.getText();
                String json = String.format(
                        "{ \"name\": \"%s\", \"password\": \"%s\"}",
                        escapeJson(name), escapeJson(pass)
                );
                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                new Thread(() -> {
                    try {
                        HttpResponse<String> response = client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString()
                        );

                        Platform.runLater(() -> {
                            try {
                                logger.info("Status code: " + response.statusCode());
                                logger.info("Response body: " + response.body());
                                if (response.statusCode() >= 200 && response.statusCode() < 300) {

                                    ObjectMapper mapper = new ObjectMapper();
                                    UserResponse user = mapper.readValue(response.body(), UserResponse.class);

                                    Session.setUser(user);
                                    AppContext.getInstance().setUserId(user.getId());
                                    String fxmlpath = "/com/auction/client/fxml/seller/main-layout.fxml";
                                    if (user.getRoles() !=null && user.getRoles().contains("ADMIN")){
                                        logger.info("Admin account allowed!");
                                        fxmlpath = "/com/auction/client/fxml/Admin/AdminDashboard.fxml";
                                    }
                                    FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlpath));
                                    Parent root = loader.load();

                                    Stage stage = ownerStage != null ? ownerStage : resolveStage();
                                    boolean newStage = stage == null;
                                    if (newStage) stage = new Stage();
                                    stage.setScene(new Scene(root));
                                    if (newStage) {
                                        stage.show();
                                    }

                                } else {
                                    String message = response.body() == null || response.body().isBlank()
                                            ? "Invalid username or password!"
                                            : response.body();
                                    passwordError.setText(message);
                                }
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "Gặp lỗi xử lý dữ liệu sau khi nhận phản hồi đăng nhập thành công từ server.", e);
                            }
                        });

                    } catch (Exception e) {
                        logger.log(Level.SEVERE, "Lỗi kết nối mạng trong quá trình gửi luồng đăng nhập bất đồng bộ.", e);
                        Platform.runLater(() -> passwordError.setText("Cannot connect to server."));
                    }
                }).start();

            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "Gặp ngoại lệ khi đóng gói payload và khởi tạo tiến trình đăng nhập.", e);
            }
        }
    }

    private Stage resolveStageFromEvent(ActionEvent event) {
        if (event == null) return null;
        Object src = event.getSource();
        if (!(src instanceof Node node)) return null;
        if (node.getScene() == null) return null;
        Window window = node.getScene().getWindow();
        return window instanceof Stage stage ? stage : null;
    }

    private Stage resolveStage() {
        for (Node node : new Node[]{loginButton, username, userpassword}) {
            if (node != null && node.getScene() != null) {
                Window window = node.getScene().getWindow();
                if (window instanceof Stage stage) {
                    return stage;
                }
            }
        }
        return null;
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