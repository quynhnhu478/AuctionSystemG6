package com.auction.server.controller;

import com.auction.common.payload.SellerRegistrationRequest;
import com.auction.common.payload.SellerRegistrationResponse;
import com.auction.server.service.SellerRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seller")
public class SellerRegistrationController {
    private final SellerRegistrationService sellerRegistrationService;

    public SellerRegistrationController(SellerRegistrationService sellerRegistrationService){
        this.sellerRegistrationService = sellerRegistrationService;
    }
    @PostMapping("/register")
    public ResponseEntity<SellerRegistrationResponse> registerAsSeller(@RequestHeader Long userId,
                                                                       @RequestBody SellerRegistrationRequest request){
         SellerRegistrationResponse response = sellerRegistrationService.registerAsSeller(userId,request);
         return ResponseEntity.ok(response);
    }
}
