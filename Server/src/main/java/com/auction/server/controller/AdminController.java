package com.auction.server.controller;

import com.auction.common.payload.*;
import com.auction.server.model.user.SellerRegistration;
import com.auction.server.service.AuthService;
import com.auction.server.service.ItemService;
import com.auction.server.service.SellerRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    // Khởi tạo Logger chẩn đoán theo chuẩn SLF4J cho Spring Boot Controller
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final SellerRegistrationService sellerRegistrationService;
    private final AuthService authService;
    private final ItemService itemService;

    public AdminController(SellerRegistrationService sellerRegistrationService,
                           AuthService authService,
                           ItemService itemService) {
        this.sellerRegistrationService = sellerRegistrationService;
        this.authService = authService;
        this.itemService = itemService;
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
            log.error("Gặp ngoại lệ khi xử lý phê duyệt/từ chối đơn đăng ký Seller: {}", e.getMessage(), e);
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
            log.error("Không thể lấy danh sách người dùng cho Admin do lỗi hệ thống nội bộ.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/seller-registration1/{userId}")
    public ResponseEntity<SellerRegistrationResponse> getSellerRegistrationDetail(@PathVariable Long userId) {
        log.info("Yêu cầu lấy chi tiết đơn đăng ký Seller cho mã người dùng (Userid): {}", userId);

        // Gọi xuống Service để lấy chi tiết đơn hàng dựa vào userId
        SellerRegistrationResponse response = sellerRegistrationService.getRegistrationDetailByUserId(userId);

        // Trả về dữ liệu cho JavaFX kèm HTTP Status 200 OK
        return ResponseEntity.ok(response);
    }
    @GetMapping("/product_list")  //lấy danh sách tất cả sản phẩm
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        try{
            log.info("Lấy danh dách sản phẩm cho admin");
            List<ItemResponse> itemList = itemService.getAllItemResponses();
            return ResponseEntity.ok(itemList);
        }
        catch (Exception e){
            log.error("không thể lấy danh sách sản phẩm");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/product/{itemId}")
    public ResponseEntity<ItemResponse> getItemDetail(@PathVariable Long itemId) {
        log.info("Yêu cầu lấy chi tiết đơn đăng ký Seller cho mã người dùng (Itemid): {}", itemId);

        // Gọi xuống Service để lấy chi tiết đơn hàng dựa vào userId
        ItemResponse response = itemService.ItemDetail(itemId);

        // Trả về dữ liệu cho JavaFX kèm HTTP Status 200 OK
        return ResponseEntity.ok(response);
    }
}