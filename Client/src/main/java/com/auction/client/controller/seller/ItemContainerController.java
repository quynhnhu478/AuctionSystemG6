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
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.auction.client.service.AlertService.showAlert;

public class ItemContainerController {
    // Khởi tạo Logger dùng để ghi nhận log chẩn đoán lỗi cho class
    private static final Logger logger = Logger.getLogger(ItemContainerController.class.getName());

    @FXML
    private GridPane itemContainer;

    // Biến tọa độ toàn cục dùng để quản lý vị trí sắp xếp các ô (cell) trong lưới GridPane một cách chính xác
    private int currentColumn = 0;
    private int currentRow = 0;

    @FXML
    public void initialize() {
        AppContext.getInstance().setItemContainerController(this);
    }

    @FXML
    public void addNewCardToGrid(Long id, String title, String description, String category, double price, double bidIncrement, LocalDateTime startingTime, LocalDateTime endTime, String localImagePath) {
        try {
            // Tải thành phần giao diện khuôn mẫu (layout) cho thẻ sản phẩm (item card)
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/fxml/seller/card-item.fxml"));
            VBox itemCardNode = fxmlLoader.load();

            // Đổ các dữ liệu thuộc tính vào các trường hiển thị của lớp điều khiển card ứng với view model
            CardItemController cardController = fxmlLoader.getController();
            cardController.setData(id, title, description, category, price, bidIncrement, startingTime, endTime, localImagePath);

            // Thêm nút thành phần card sản phẩm vào đúng vị trí tọa độ mục tiêu trong lưới một cách an toàn
            // Lưu ý: Đã sửa lại lỗi đảo ngược vị trí cấu trúc từ (currentRow, currentColumn) cho khớp với quy tắc chuẩn của GridPane
            itemContainer.add(itemCardNode, currentColumn, currentRow);

            // Tính toán vị trí ô tiếp theo cho lượt gán tọa độ sắp xếp kế tiếp
            currentColumn++;
            if (currentColumn > 3) {  // Giới hạn lưới tối đa 4 cột (Chỉ số index tương ứng: 0, 1, 2, 3)
                currentColumn = 0;    // Đưa con trỏ cột quay trở lại vị trí ô đầu tiên bên trái ngoài cùng
                currentRow++;         // Tăng chỉ số hàng để chuyển xuống dòng tiếp theo bên dưới
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Gặp lỗi phân tích thành phần cấu trúc bên trong khi đang vẽ các khung nhìn giao diện.", e);
            showAlert(Alert.AlertType.ERROR, "Load Error", "The item card could not be loaded: " + e.getMessage());
        }
    }

    // Khối quy tắc xử lý dồn hàng và tái cấu trúc lại vị trí các ô trong lưới sau khi xóa phần tử
    public void refreshGridAfterDelete(VBox deletedCardNode) {
        // Thu thập toàn bộ các instance card giao diện hiện tại đang nằm trong thành phần chứa active
        List<Node> remainingCards = new ArrayList<>(itemContainer.getChildren());

        // Loại bỏ phần tử node giao diện tương ứng với sản phẩm vừa bị xóa ra khỏi danh sách theo dõi
        remainingCards.remove(deletedCardNode);

        // Xóa sạch toàn bộ các layout thành phần giao diện cũ đang hiển thị trên lưới GridPane
        itemContainer.getChildren().clear();

        // Đặt lại các biến đếm tọa độ giám sát không gian vị trí ban đầu
        currentColumn = 0;
        currentRow = 0;

        // Tiến hành lặp và tái cấu trúc nén layout, thiết lập lại các chỉ số tọa độ mới cho các card còn lại
        for (Node card : remainingCards) {
            itemContainer.add(card, currentColumn, currentRow);

            currentColumn++;
            if (currentColumn > 3) {
                currentColumn = 0;
                currentRow++;
            }
        }

        logger.info("[UI] Các ô item trong container động đã được sắp xếp dồn hàng và tái hiển thị an toàn sau khi thay đổi thực thể.");
    }
}