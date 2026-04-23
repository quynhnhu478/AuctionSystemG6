package org.example.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SignUpController {
    @FXML
    public void switchToSignInButton(ActionEvent event) throws IOException {
        // Giả sử kiểm tra email/pass đúng...
        Parent loginView = FXMLLoader.load(getClass().getResource("/org/example/fxml/signin.fxml"));
        Scene scene = new Scene(loginView);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }
}
