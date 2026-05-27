package com.auction.client.controller;

import com.auction.client.service.WebSocketClientService;
import com.auction.common.payload.ItemResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.lang.reflect.Type;

public class HomeViewController {
    private static final Logger log = LoggerFactory.getLogger(HomeViewController.class);
    @FXML
    private GridPane liveGridPane;

    private StompSession stompSession;

    @FXML
    public void initialize(){
        subcribeToLiveAuctions();
    }

    public void subcribeToLiveAuctions(){
        WebSocketClientService.getInstance().doOnConnect(session -> {
            // 2. Tiến hành "Số máy lẻ" - Đăng ký vào kênh live-auctions
            session.subscribe("/topic/live-auctions", new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        String json = (String) payload;
                        ObjectMapper objectMapper = new JsonMapper().builder().addModule(new JavaTimeModule()).build();
                        ItemResponse itemResponse = objectMapper.readValue(json, ItemResponse.class);

                        Platform.runLater(() -> {
                            addNewItemToLiveGrid(itemResponse);
                        });
                    }
                });
            log.info("Subcribe to Live Auctions");
            });
    }

    private int liveColumn = 0;
    private int liveRow = 0;
    private final int MAX_COLUMNS = 4;
    private void addNewItemToLiveGrid(ItemResponse newItem) {
        try {
            // Tải file fxml của card hiển thị đấu giá (Ví dụ: card-item hoặc live-card tùy bạn thiết kế)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/auction/ProductCard.fxml"));
            Parent cardNode = loader.load();

            ProductCardController cardController = loader.getController();

            // Chuẩn hóa đường dẫn ảnh từ thuộc tính savedFileName của phản hồi
            String fullImageUrl = "http://localhost:8080/uploads/items/" + newItem.getSavedFileName();

            // Đổ dữ liệu real-time vừa nhận qua mạng vào card
            cardController.setData(
                    newItem.getId(),
                    newItem.getName(),
                    newItem.getDescription(),
                    newItem.getCategories().toString(), // truyền tên file ảnh
                    newItem.getPrice(),
                    newItem.getStartingTime(),
                    newItem.getEndTime(),
                    fullImageUrl);

            // Đưa vào lưới GridPane hiển thị của trang Live Auctions công khai
            liveGridPane.add(cardNode, liveColumn, liveRow);

            // Tính toán tịnh tiến ô tiếp theo
            liveColumn++;
            if (liveColumn >= MAX_COLUMNS) {
                liveColumn = 0;
                liveRow++;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
