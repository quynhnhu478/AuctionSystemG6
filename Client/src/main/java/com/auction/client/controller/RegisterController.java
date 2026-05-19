package com.auction.client.controller;

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


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RegisterController {
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
        SceneService.changeScene(actionEvent, "/com/auction/client/fxml/login.fxml");
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
            System.out.println("Error");
            Notifications.create()
                    .title("Error")
                    .text("Please fill all the blankets")
                    .showError();

        }
        else{
            try{
                String name = username.getText();
                String email = useremail.getText();
                String password = userpassword.getText();

                String json = String.format(
                        "{ \"name\": \"%s\", \"email\": \"%s\", \"password\": \"%s\"}",
                        name, email, password
                );
                System.out.println("JSON gửi đi: " + json);
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8080/api/auth/register"))
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
                                   System.out.println("Status code: " + response.statusCode());
                                   System.out.println("Response body: " + response.body());
                                   if (response.statusCode() >= 200 && response.statusCode() < 300 ){
                                       Notifications.create()
                                               .title("Success")
                                               .text("Register successful!")
                                               .showInformation();
                                       FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/login.fxml"));
                                       Parent root = loader.load();

                                       Stage stage = (Stage) registerButton.getScene().getWindow();
                                       stage.setScene(new Scene(root));
                                   }
                                   else {
                                       Notifications.create()
                                               .title("Error")
                                               .text("Register failed!")
                                               .showError();
                                   }
                               }
                               catch (Exception e){
                                   e.printStackTrace();
                               }

                       });
                   }
                   catch (Exception e){
                       e.printStackTrace();
                   }
               }).start();

            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }


}
