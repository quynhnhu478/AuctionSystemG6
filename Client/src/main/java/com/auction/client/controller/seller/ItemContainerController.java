package com.auction.client.controller.seller;

import com.auction.client.service.AppContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    public void addNewCardToGrid(Long id, String title, String description, String category, double price, LocalDateTime startingTime, LocalDateTime endTime, String localImagePath) {
        try {
            //load khuôn mẫu card item
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/card-item.fxml"));
            VBox itemCardNode = fxmlLoader.load();  //khởi tạo giao diện của Card

            //thêm dữ liệu
            CardItemController cardController = fxmlLoader.getController();
            cardController.setData(id, title, description, category, price, startingTime, endTime, localImagePath);

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

    //hàm xử lý dồn hàng
    public void refreshGridAfterDelete(VBox deletedCardNode){
        //gom tất cả các card đàn hiển thị vào một list
        //copy thành 1 ArrayList v getChildren() của JavaFx là 1 ObservableList
        List<Node> remainingCards = new ArrayList<>(itemContainer.getChildren());

        //xóa card vừa bị xóa ra khỏi danh sách
        remainingCards.remove(deletedCardNode);

        //xóa toàn bộ giao diện cũ ở gridpane
        itemContainer.getChildren().clear();

        //reset lại tọa độ con trỏ hàng và cột
        currentColumn = 0;
        currentRow = 0;

        //tự động dồn hàng
        for(Node card: remainingCards){
            itemContainer.add(card,currentRow,currentColumn);

            currentColumn++;
            if(currentColumn > 3) {
                currentColumn = 0;
                currentRow++;
            }
        }
    }
}