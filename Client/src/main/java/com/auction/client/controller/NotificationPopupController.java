package com.auction.client.controller;

import com.auction.client.service.NotificationStore;
import com.auction.common.payload.NotificationMessage;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotificationPopupController {
    @FXML
    private VBox notificationList;
    @FXML
    private Label emptyLabel;

    @FXML
    private void initialize() {
        render();
        NotificationStore.markAllRead();
    }

    private void render() {
        notificationList.getChildren().clear();
        if (NotificationStore.getAll().isEmpty()) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            return;
        }
        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);
        for (NotificationMessage notification : NotificationStore.getAll()) {
            notificationList.getChildren().add(createRow(notification));
        }
    }

    private VBox createRow(NotificationMessage notification) {
        Label title = new Label(notification.getTitle() == null ? "Notification" : notification.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        Label message = new Label(notification.getMessage() == null ? "" : notification.getMessage());
        message.setWrapText(true);
        message.setStyle("-fx-font-size: 12; -fx-text-fill: #475569;");

        Label time = new Label(formatTime(notification.getCreatedAt()));
        time.setStyle("-fx-font-size: 11; -fx-text-fill: #94A3B8;");

        VBox row = new VBox(4, title, message, time);
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8; -fx-padding: 10 12 10 12;");
        return row;
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return time.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
}
