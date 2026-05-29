package com.auction.server.controller;

import com.auction.common.payload.LoginRequest;
import com.auction.common.payload.RegisterRequest;
import com.auction.common.payload.UserResponse;
import com.auction.server.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    // Khởi tạo Logger theo chuẩn SLF4J cho Spring Boot Controller
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @PostMapping("/register")
    public UserResponse register(@RequestBody RegisterRequest request){
        return authService.register(request);
    }

    @PostMapping("/login")
    public UserResponse login(@RequestBody LoginRequest request){
        return authService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestParam Long userId){
        log.info("Check logout cho user id: {}", userId);
        try{
            return ResponseEntity.ok("Logout successfully");
        }
        catch (Exception e){
            log.error("Gặp ngoại lệ bất ngờ khi xử lý đăng xuất cho người dùng mã số {}: ", userId, e);
            return ResponseEntity.internalServerError().body("Logout failed");
        }
    }

    @PutMapping("/update_balance")
    public ResponseEntity<UserResponse> updateBalance(@RequestParam("userId") Long userId,
                                                      @RequestParam("balance") double balance){
         log.info("Check update balance cho user id: {}, số dư mới: {}", userId, balance);
         UserResponse userResponse = authService.updateUserBalance(userId, balance);
         return ResponseEntity.ok(userResponse);
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserResponse> getProfile(@PathVariable Long userId) {
        log.info("Lấy thông tin profile mới nhất cho user id: {}", userId);
        UserResponse userResponse = authService.getProfile(userId);
        return ResponseEntity.ok(userResponse);
    }
}