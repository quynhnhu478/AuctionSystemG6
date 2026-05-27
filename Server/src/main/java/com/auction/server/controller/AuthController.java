package com.auction.server.controller;

import com.auction.common.payload.LoginRequest;
import com.auction.common.payload.RegisterRequest;
import com.auction.common.payload.UserResponse;
import com.auction.server.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
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
        System.out.println("Check logout cho user id: " + userId);
        try{
            return ResponseEntity.ok("Logout successfully");
        }
        catch (Exception e){
            return ResponseEntity.internalServerError().body("Logout failed");
        }

    }
    @PutMapping("/update_balance")
    public ResponseEntity<UserResponse> updateBalance(@RequestParam("userId") Long userId,
                                                      @RequestParam("balance") double balance){
        System.out.println("Check update balance cho user id: " + userId);
        UserResponse userResponse = authService.updateUserBalance(userId, balance);
        return ResponseEntity.ok(userResponse);
    }
}
