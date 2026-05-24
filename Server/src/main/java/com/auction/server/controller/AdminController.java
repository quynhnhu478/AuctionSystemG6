package com.auction.server.controller;

import com.auction.common.payload.HandleSellerRegistrationRequest;
import com.auction.common.payload.SellerRegistrationRequest;
import com.auction.common.payload.SellerRegistrationResponse;
import com.auction.common.payload.UserResponse;
import com.auction.server.model.user.SellerRegistration;
import com.auction.server.service.AuthService;
import com.auction.server.service.SellerRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final SellerRegistrationService sellerRegistrationService;
    private final AuthService authService;

    public AdminController(SellerRegistrationService sellerRegistrationService, AuthService authService) {
        this.sellerRegistrationService = sellerRegistrationService;
        this.authService = authService;
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
    @GetMapping("/user_list")
    public ResponseEntity<List<UserResponse>> getAllUsersForAdmin() {
        try {
            List<UserResponse> users = authService.usersList();
            // Nếu danh sách trống, bạn vẫn trả về 200 OK kèm mảng rỗng []
            // để phía JavaFX không bị lỗi crash giao diện
            return ResponseEntity.ok(users);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/seller-registration1/{userId}")
    public ResponseEntity<SellerRegistrationResponse> getSellerRegistrationDetail(@PathVariable Long userId) {

        // Gọi xuống Service để lấy chi tiết đơn hàng dựa vào userId
        SellerRegistrationResponse response = sellerRegistrationService.getRegistrationDetailByUserId(userId);

        // Trả về dữ liệu cho JavaFX kèm HTTP Status 200 OK
        return ResponseEntity.ok(response);
    }
}
