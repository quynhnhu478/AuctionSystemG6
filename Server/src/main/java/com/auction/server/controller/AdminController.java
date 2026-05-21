package com.auction.server.controller;

import com.auction.common.payload.HandleSellerRegistrationRequest;
import com.auction.common.payload.SellerRegistrationRequest;
import com.auction.common.payload.SellerRegistrationResponse;
import com.auction.server.service.SellerRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/handle_sellerRegistration")
    public ResponseEntity<?> handleSellerRegistration(@RequestHeader("X-Role") String role,
                                                      @RequestBody HandleSellerRegistrationRequest request){
        if (!"ADMIN".equals(role)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Access denied! Admin role required!");
        }
        try{
            sellerRegistrationService.handleSellerRegistration(request);
            return ResponseEntity.ok("Handled successfully!");
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }
}
