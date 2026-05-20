package com.auction.server.controller;

import com.auction.server.payload.User.SellerRegistrationResponse;
import com.auction.server.service.SellerRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final SellerRegistrationService sellerRegistrationService;

    public AdminController(SellerRegistrationService sellerRegistrationService){
        this.sellerRegistrationService = sellerRegistrationService;
    }
    @GetMapping("/seller_registration")
    public ResponseEntity<?> getPendingList(@RequestHeader("X-Role") String role){
        if (!"ADMIN".equals(role)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied! Admin role required!");
        }
        List<SellerRegistrationResponse> pendingList = sellerRegistrationService.getPendingRegistration();
        return ResponseEntity.ok(pendingList);
    }
}
