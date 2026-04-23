package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SignInController {
    @FXML
    public void switchToSignUpLink(ActionEvent event) throws IOException {
        // Giả sử kiểm tra email/pass đúng...
        Parent signUpView = FXMLLoader.load(getClass().getResource("/org/example/fxml/signup.fxml"));
        Scene scene = new Scene(signUpView);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }
}
