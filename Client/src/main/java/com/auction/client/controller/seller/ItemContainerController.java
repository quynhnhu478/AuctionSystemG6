package com.auction.client.controller.seller;

import com.auction.client.service.AppContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDateTime;

import static com.auction.client.service.AlertService.showAlert;

public class ItemContainerController {
    @FXML
    private GridPane itemContainer;

    //Biến đếm vị trí hang và cột toàn cục của trang này để card không bị đè nhau
    private int currentColumn = 0;
    private int currentRow = 0;

    @FXML
    public void initialize() {
        AppContext.getInstance().setItemContainerController(this);
    }
    @FXML
    public void addNewCardToGrid(String title, String description, String category, double price, LocalDateTime startingTime, LocalDateTime endTime, String localImagePath) {
        try {
            //load khuôn mẫu card item
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/card-item.fxml"));
            VBox itemCardNode = fxmlLoader.load();  //khởi tạo giao diện của Card

            //thêm dữ liệu
            CardItemController cardController = fxmlLoader.getController();
            cardController.setData(title, description, category, price, startingTime, endTime, localImagePath);

            //thêm thẻ vào gridpane theo vị trí chuẩn
            itemContainer.add(itemCardNode,currentRow,currentColumn);

            //tự động tính toa tọa độ cho chiếc card tiếp theo
            currentColumn++;
            if(currentColumn > 3) {  //lưới có 4 cột, nếu vượt quá cột 3 thì xuống dònng
                currentColumn = 0;   //quay về cột đầu tiên bên trái
                currentRow++;   //xuống hàng tiếp theo bên dưới
            }
        }catch(IOException e){
            showAlert(Alert.AlertType.ERROR, "The item card could not be loaded", e.getMessage());
        }
    }
}
