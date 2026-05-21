package com.auction.client.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("/com/auction/client/fxml/signin/hello.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Auction Hub");
        stage.setScene(scene);
        stage.show();
    }
}
