package com.auction.server.controller;

import com.auction.server.model.User;
import com.auction.server.payload.LoginRequest;
import com.auction.server.payload.RegisterRequest;
import com.auction.server.payload.UserResponse;
import com.auction.server.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
