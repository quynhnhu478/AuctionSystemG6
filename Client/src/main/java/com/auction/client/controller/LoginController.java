package com.auction.client.controller;

import com.auction.client.service.SceneService;
import com.auction.client.service.Session;
import com.auction.client.service.UserResponse;
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
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class LoginController {
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
    public void switchToRegister(ActionEvent actionEvent){
        SceneService.changeScene(actionEvent, "/com/auction/client/fxml/register.fxml");
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
                    .text("Please fill all the blankets")
                    .showError();
        }
        else{
            try{
                String name = username.getText();
                String pass = userpassword.getText();
                String json = String.format(
                        "{ \"name\": \"%s\", \"password\": \"%s\"}",
                        name, pass
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
                                if (response.statusCode() == 200) {

                                    ObjectMapper mapper = new ObjectMapper();
                                    UserResponse user = mapper.readValue(response.body(), UserResponse.class);

                                    Session.setUser(user);

                                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/profile.fxml"));
                                    Parent root = loader.load();

                                    Stage stage = (Stage) loginButton.getScene().getWindow();
                                    stage.setScene(new Scene(root));

                                } else {
                                    passwordError.setText("Invalid username or password!");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();



            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
