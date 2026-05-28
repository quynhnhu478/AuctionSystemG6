package com.auction.client.controller.seller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;

public class TermsConditionsController {

    private CheckBox targetCheckBox; // Tham chiếu đến CheckBox đồng ý điều khoản ở giao diện trước

    // Nhận tham chiếu CheckBox mục tiêu để cập nhật trạng thái khi người dùng tương tác
    public void setTargetCheckBox(CheckBox targetCheckBox) {
        this.targetCheckBox = targetCheckBox;
    }

    // Xử lý sự kiện khi người dùng từ chối (Decline) điều khoản
    @FXML
    private void handleDecline(ActionEvent event) {
        if (targetCheckBox != null) {
            targetCheckBox.setSelected(false); // Hủy chọn CheckBox ở form đăng ký
        }
        closeDialog(event);
    }

    // Xử lý sự kiện khi người dùng chấp nhận (Accept) điều khoản
    @FXML
    private void handleAccept(ActionEvent event) {
        if (targetCheckBox != null) {
            targetCheckBox.setSelected(true); // Tự động tích chọn CheckBox ở form đăng ký
        }
        closeDialog(event);
    }

    // Hàm trợ giúp đóng cửa sổ dialog hiện tại dựa trên sự kiện kích hoạt
    private void closeDialog(ActionEvent event) {
        // Truy vết nguồn phát sinh sự kiện -> lấy Scene -> đóng Stage bọc ngoài cùng
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}