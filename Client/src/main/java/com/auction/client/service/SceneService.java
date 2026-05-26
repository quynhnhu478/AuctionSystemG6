package com.auction.client.service;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneService {
    public static void changeScene(ActionEvent event, String fxmlPath){
        try{
            Parent root = FXMLLoader.load(SceneService.class.getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        }
        catch (IOException e){
            System.out.println("Khong the chuyen trang "+ e.getMessage());
        }
    }
}