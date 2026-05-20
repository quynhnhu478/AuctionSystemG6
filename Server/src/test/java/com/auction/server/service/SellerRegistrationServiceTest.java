package com.auction.server.service;

import com.auction.server.exception.AuthException;
import com.auction.server.model.User.SellerRegistration;
import com.auction.server.payload.User.SellerRegistrationRequest;
import com.auction.server.payload.User.SellerRegistrationResponse;
import com.auction.server.repository.SellerRegistrationRepository;
import com.auction.server.util.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


@SpringBootTest
@Transactional
@ActiveProfiles("test")
class SellerRegistrationServiceTest {
    @Autowired
    private SellerRegistrationService sellerRegistrationService;

    @Autowired
    private SellerRegistrationRepository sellerRegistrationRepository;

    @Mock
    private FileStorageService fileStorageService;

    private SellerRegistrationRequest request;
    private Long userId;


    @BeforeEach
    void setUp(){

        request = new SellerRegistrationRequest();
        request.setName("Shop A");
        request.setEmail("nhi@gmail.com");
        request.setAddress("Ha Noi");
        request.setIdentityNumber("0123456");
        request.setPhoneNumber("012345678");
        request.setIdentifiedImageFront("cccdMatTruoc");
        request.setIdentifiedImageBehind("cccdMatSau");
        sellerRegistrationRepository.deleteAll();

    }
    @AfterEach
    void tearDown() {
        File uploadsDir = new File("cccd");
        if (uploadsDir.exists()) {
            deleteDirectory(uploadsDir);
        }
    }
    @Test
    @DisplayName("Test Seller Registration Pending - Fall")
    void testPendingOrder() {
        SellerRegistration pendingOrder = new SellerRegistration();
        pendingOrder.setStatus("PENDING");
        sellerRegistrationRepository.save(pendingOrder);

        AuthException exception = assertThrows(AuthException.class, () ->{
            sellerRegistrationService.registerAsSeller(userId, request);
        });
        assertEquals("Your registration is pending admin approval!", exception.getMessage());
    }
    @Test
    @DisplayName("Test Seller Registration approval- Fall")
    void testApprovalOrder() {
        SellerRegistration approvedOrder = new SellerRegistration();
        approvedOrder.setStatus("APPROVED");
        sellerRegistrationRepository.save(approvedOrder);

        AuthException exception = assertThrows(AuthException.class, () ->{
            sellerRegistrationService.registerAsSeller(userId, request);
        });
        assertEquals("Your account is already registered as a seller!", exception.getMessage());
    }
    @Test
    @DisplayName("Test Seller Registration - Success")
    void testSellerRegistration(){

        sellerRegistrationService.registerAsSeller(userId,request);

        boolean isSavedInDB = sellerRegistrationRepository.existsByUser1_IdAndStatus(userId, "PENDING");
        assertTrue(isSavedInDB, "Dữ liệu đăng ký phải được lưu vào Database ảo với trạng thái PENDING");
    }

    private void deleteDirectory(File directorytoBeDeleted) {
        File[] allContents = directorytoBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directorytoBeDeleted.delete();
    }
}