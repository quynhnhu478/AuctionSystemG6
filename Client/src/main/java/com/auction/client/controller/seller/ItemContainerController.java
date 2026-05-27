package com.auction.client.controller.seller;

import com.auction.client.controller.MainLayoutController;
import com.auction.client.service.AppContext;
import com.auction.client.service.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.auction.client.service.AlertService.showAlert;

public class ItemContainerController {
    private static final Logger log = LoggerFactory.getLogger(ItemContainerController.class);
    @FXML
    private GridPane itemContainer;

    //Biến đếm vị trí hang và cột toàn cục của trang này để card không bị đè nhau
    private int currentColumn = 0;
    private int currentRow = 0;

    //biến toàn cục để quản lý riêng tấm card nút bấm
    private VBox addCardNode = null;

    @FXML
    public void initialize() {
        AppContext.getInstance().setItemContainerController(this);
        try{
            FXMLLoader addCardLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/add-item-card.fxml"));
            addCardNode = addCardLoader.load();
        }catch(IOException e){
            log.error("Haven't created add-item-card.fxml file yet");
        }

        loadAllItemFromServer();
    }

    @FXML
    public void addNewCardToGrid(Long id, String title, String description, String category, double price, LocalDateTime startingTime, LocalDateTime endTime, String localImagePath) {
        try {
            //nếu trên lưới có sẵn nút add new ở ô cuối, gỡ nó ra để thêm sản phẩm
            if(addCardNode != null){
                itemContainer.getChildren().remove(addCardNode);
            }

            //load khuôn mẫu card item
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/card-item.fxml"));
            VBox itemCardNode = fxmlLoader.load();  //khởi tạo giao diện của Card

            //thêm dữ liệu
            CardItemController cardController = fxmlLoader.getController();
            cardController.setData(id, title, description, category, price, startingTime, endTime, localImagePath);

            //thêm thẻ vào gridpane theo vị trí chuẩn
            itemContainer.add(itemCardNode, currentColumn, currentRow);

            //tự động tính toa tọa độ cho chiếc card tiếp theo
            currentColumn++;
            if (currentColumn > 3) {  //lưới có 4 cột, nếu vượt quá cột 3 thì xuống dònng
                currentColumn = 0;   //quay về cột đầu tiên bên trái
                currentRow++;   //xuống hàng tiếp theo bên dưới
            }

            //đẩy nút bấm "+add new" vào ô trống kế tiếp
            if(addCardNode != null){
                itemContainer.add(addCardNode, currentColumn, currentRow);
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "The item card could not be loaded", e.getMessage());
        }
    }

    //hàm xử lý dồn hàng
    public void refreshGridAfterDelete(VBox deletedCardNode) {
        //gom tất cả các card đàn hiển thị vào một list
        //copy thành 1 ArrayList v getChildren() của JavaFx là 1 ObservableList
        List<Node> remainingCards = new ArrayList<>(itemContainer.getChildren());

        //xóa card vừa bị xóa ra khỏi danh sách
        remainingCards.remove(deletedCardNode);
        if(addCardNode != null){
            remainingCards.remove(addCardNode);
        }

        //Nếu không còn bất kỳ sản phẩm thật nào
        if(remainingCards.isEmpty()){
            itemContainer.getChildren().clear();

            //quay về luồng giao diện chính để đổi view của màn hình tổng
            try{
                MainLayoutController mainLayoutController = AppContext.getInstance().getMainLayoutController();
                if(mainLayoutController != null){
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/my-listings-view.fxml"));
                    Parent myListingView = loader.load();

                    mainLayoutController.setCenterView(myListingView);
                }
            }catch (IOException e){
                showAlert(Alert.AlertType.ERROR, "System Error", "Cannot load empty state view: " +  e.getMessage());
            }
            return; //thoát hàm, không cần dồn hàng nữa
        }

        //xóa toàn bộ giao diện cũ ở gridpane
        itemContainer.getChildren().clear();

        //reset lại tọa độ con trỏ hàng và cột
        currentColumn = 0;
        currentRow = 0;

        //tự động dồn hàng
        for (Node card : remainingCards) {
            itemContainer.add(card, currentColumn, currentRow);

            currentColumn++;
            if (currentColumn > 3) {
                currentColumn = 0;
                currentRow++;
            }
        }

        //xếp xong sản phẩm th lại thêm nút "add new"
        if(addCardNode != null){
            itemContainer.add(addCardNode, currentColumn, currentRow);
        }
    }

    public void loadAllItemFromServer(){
        itemContainer.getChildren().clear();
        this.currentColumn = 0;
        this.currentRow = 0;

        Long currentUserId = Session.getUser().getId();
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/items/my-listings?userId=" + currentUserId))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if(response.statusCode() == 200){
                        Platform.runLater(()->{
                            try{
                                JsonNode rootNode = new JsonMapper().builder()
                                        .addModules(new JavaTimeModule())
                                        .build()
                                        .readTree(response.body());

                                itemContainer.getChildren().clear();

                                if(rootNode.isArray()){
                                    for(JsonNode itemNode : rootNode){
                                        //lấy tên file ảnh từ JSON Server trả về
                                        String savedFileName = itemNode.get("savedFileName").asText();

                                        //Nối chuỗi tạo thành đường link URL đi qua server
                                        String fullImageUrl = "http://localhost:8080/uploads/items/" + savedFileName;
                                        addNewCardToGrid(
                                                itemNode.get("id").asLong(),
                                                itemNode.get("name").asText(),
                                                itemNode.get("description").asText(),
                                                itemNode.get("categories").asText(),
                                                itemNode.get("price").asDouble(),
                                                LocalDateTime.parse(itemNode.get("startingTime").asText()),
                                                LocalDateTime.parse(itemNode.get("endTime").asText()),
                                                fullImageUrl
                                        );
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
    }
}