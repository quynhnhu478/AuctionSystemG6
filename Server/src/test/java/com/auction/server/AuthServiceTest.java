package com.auction.server;

import com.auction.server.exception.AuthException;
import com.auction.server.model.Roles;
import com.auction.server.model.User;
import com.auction.server.payload.LoginRequest;
import com.auction.server.payload.RegisterRequest;
import com.auction.server.payload.UserResponse;
import com.auction.server.repository.RoleRepository;
import com.auction.server.repository.UserRepository;
import com.auction.server.service.AuthService;
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
    private User user;
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
    @DisplayName("Test Register Request - Fall")
    void testRegister_Fall(){
        authService.register(registerRequest);

        AuthException exception = assertThrows(AuthException.class, () -> authService.register(registerRequest));

        assertEquals("Username already exists!", exception.getMessage());
    }
}
