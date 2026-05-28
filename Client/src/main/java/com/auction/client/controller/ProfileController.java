package com.auction.client.controller;

import com.auction.client.service.Session;
import com.auction.common.payload.UserResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfileController {
    @FXML
    private Label username; // Nhãn hiển thị tên tài khoản người dùng
    @FXML
    private Label useremail; // Nhãn hiển thị thông tin email/vai trò

    @FXML
    public void initialize(){
        // Lấy thông tin tài khoản người dùng đang đăng nhập từ Session hiện tại
        UserResponse user = Session.getUser();

        // Nếu thông tin người dùng tồn tại, tiến hành cập nhật dữ liệu lên giao diện
        if (user != null) {
            username.setText("Hello " + user.getName());
            useremail.setText("Role: " + user.getEmail());
        }
    }
}