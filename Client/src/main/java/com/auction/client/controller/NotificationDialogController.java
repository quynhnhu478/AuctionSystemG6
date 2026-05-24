package com.auction.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.util.List;

public class NotificationDialogController {
    @FXML
    private ListView<String> notificationListView;

    public void setNotification(List<String> notification){
        //Chuyển List thường thành ObservableList để gán vào ListView
        ObservableList<String> items = FXCollections.observableArrayList(notification);
        notificationListView.setItems(items);

        //CUSTOM CELL: để chữ tự động xuống hàng nếu thông báo quá dài
        notificationListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item == null){
                    setText(null);
                    setGraphic(null);
                }
                else{
                    setText(item);
                    setWrapText(true);  //tự động xuống dòng
                    setStyle("-fx-padding: 10; -fx-border-color: transparent transparent #f1f3f5 transparent; -fx-font-size: 13px");
                }
            }
        });
    }
}
