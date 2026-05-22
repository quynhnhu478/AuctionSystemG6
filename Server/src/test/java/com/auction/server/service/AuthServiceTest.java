package com.auction.server.service;

import com.auction.server.exception.AuthException;

import com.auction.common.payload.LoginRequest;
import com.auction.common.payload.RegisterRequest;
import com.auction.common.payload.UserResponse;
import com.auction.server.model.user.Roles;
import com.auction.server.repository.RoleRepository;
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")  //kích hoạt profile test để kết nối H2 Database
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private Roles roles;

    @BeforeEach
    void setUp() {
        //khởi tạo dữ liệu giả cho Register
        registerRequest = new RegisterRequest();
        registerRequest.setName("testUser");
        registerRequest.setEmail("test@gmail.com");
        registerRequest.setPassword("123456");

        //khởi tạo dữ liệu cho Login
        loginRequest = new LoginRequest();
        loginRequest.setName("testUser");
        loginRequest.setPassword("123456");

        //Vì dùng DB thật, ta phải tạo role sẵn trong DB
        roles = new Roles();
        roles.setRolename("BIDDER");
        roleRepository.save(roles); //lưu xuống DB H2
    }

    @Test
    @DisplayName("Test Register Request - Success")
    void testRegister_Success(){
        UserResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("testUser", response.getName());

        //kiểm tra xem dữ liệu có thực sự vào DB ko
        assertNotNull(userRepository.findByName("testUser"));
    }

    @Test
    @DisplayName("Test Register Request - Fail")
    void testRegister_Fail(){ // Đã sửa tên phương thức từ Fall thành Fail cho đúng ngữ nghĩa chính tả
        authService.register(registerRequest);

        AuthException exception = assertThrows(AuthException.class, () -> authService.register(registerRequest));

        assertEquals("Username already exists!", exception.getMessage());
    }

    @Test
    @DisplayName("Test Login Request - Success")
    void testLogin_Success(){ // THÊM MỚI: Bổ sung case test kiểm tra login khớp với dữ liệu loginRequest ở setUp
        // Đăng ký tài khoản trước để có dữ liệu đăng nhập
        authService.register(registerRequest);

        // Thực hiện hành động login
        UserResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("testUser", response.getName());
    }
}