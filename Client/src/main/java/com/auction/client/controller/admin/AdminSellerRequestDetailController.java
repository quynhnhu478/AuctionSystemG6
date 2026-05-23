package com.auction.client.controller.admin;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class AdminSellerRequestDetailController {

    //Khai báo các fx:id đồng bộ chính xác với file FXML
    @FXML
    private TextField sellerNameField;

    @FXML
    private TextField identityField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField addressField;

    @FXML
    private ImageView frontImageView;

    @FXML
    private ImageView backImageView;

    @FXML
    private Button approveButton;

    @FXML
    private Button rejectButton;

    // Biến lưu trữ đối tượng Request hiện tại
    private Object currentRequest;

    // Xử lý sự kiện khi Admin bấm nút Phê duyệt [Approve]
    @FXML
    void handleApprove(ActionEvent event) {
        System.out.println("Admin đã bấm PHÊ DUYỆT yêu cầu nâng cấp Seller!");

        //TODO: Gửi dữ liệu qua Socket/API lên Server thông báo: Duyệt thành công đối tượng này
        // Ví dụ: clientSocket.send("APPROVE_SELLER_REQUEST:" + requestId);
        closeWindow();
    }

    //Xử lý sự kiện khi Admin bấm nút Từ chối [Reject]
    @FXML
    void handleReject(ActionEvent event) {
        System.out.println("Admin clicked REJECT the Seller upgrade request!");

        //TODO: Gửi tín hiệu từ chối lên Server
        // Ví dụ: clientSocket.send("REJECT_SELLER_REQUEST:" + requestId + ":" + reason);
        closeWindow();
    }

    //Xử lý sự kiện khi Admin bấm nút Quay lại [Back to List]
    @FXML
    void handleCancel(ActionEvent event) { //
        // Đóng cửa sổ chi tiết ngay lập tức mà không thực hiện thay đổi nào
        closeWindow();
    }

    //Hàm bổ trợ dùng chung để đóng nhanh Stage (Cửa sổ) hiện tại
    private void closeWindow() {
        Stage stage = (Stage) sellerNameField.getScene().getWindow();
        stage.close();
    }
}