package com.auction.server.service;

import com.auction.common.payload.HandleSellerRegistrationRequest;
import com.auction.server.exception.AuthException;
import com.auction.server.model.user.Roles;
import com.auction.server.model.user.SellerRegistration;
import com.auction.common.enums.Status;
import com.auction.common.payload.SellerRegistrationRequest;
import com.auction.common.payload.SellerRegistrationResponse;
import com.auction.server.model.user.User;
import com.auction.server.repository.RoleRepository;
import com.auction.server.repository.SellerRegistrationRepository;
import com.auction.server.repository.UserRepository;
import com.auction.server.util.FileStorageService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SellerRegistrationService {
    // Khởi tạo Logger theo chuẩn SLF4J cho Spring Boot Service
    private static final Logger log = LoggerFactory.getLogger(SellerRegistrationService.class);

    private final SellerRegistrationRepository sellerRegistrationRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public SellerRegistrationService(SellerRegistrationRepository sellerRegistrationRepository,
                                     FileStorageService fileStorageService, UserRepository userRepository,
                                     RoleRepository roleRepository, SimpMessagingTemplate simpMessagingTemplate) {
        this.sellerRegistrationRepository = sellerRegistrationRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    public SellerRegistrationResponse registerAsSeller(Long userId, SellerRegistrationRequest sellerRegistrationRequest){
        log.info("Bắt đầu xử lý hồ sơ đăng ký Seller cho UserID: {}", userId);

        Optional<SellerRegistration> oldRegistrationOpt = sellerRegistrationRepository.findById(userId);

        //check xem tài khoản này trước đây đã từng có đơn đăng ký nào chưa
        if (oldRegistrationOpt.isPresent()){
            //th1: đơn cũ đang ở trạng thái CHỜ DUYỆT (PENDING)
            SellerRegistration oldRegistration = oldRegistrationOpt.get();

            if(Status.PENDING.toString().equals(oldRegistration.getStatus())){
                log.warn("Từ chối xử lý: UserID {} đang có một đơn đăng ký ở trạng thái chờ duyệt.", userId);
                throw new AuthException("Your registration is pending admin approval!");
            }
            else if(Status.APPROVED.toString().equals(oldRegistration.getStatus())){
                log.warn("Từ chối xử lý: UserID {} đã được duyệt quyền làm Seller trước đó.", userId);
                throw new AuthException("Your account is already registered as a seller!");
            }
        }

        // Xử lý file ảnh khi đơn bị reject (Dọn dẹp ảnh CCCD cũ nếu có đơn đăng ký lại)
        if (oldRegistrationOpt.isPresent()){
            SellerRegistration oldReg = oldRegistrationOpt.get();
            log.info("Phát hiện đơn cũ bị từ chối của UserID: {}. Tiến hành dọn dẹp tài liệu ảnh CCCD cũ.", userId);

            if (oldReg.getIdentifiedImageFront() != null){
                fileStorageService.deleteImage(oldReg.getIdentifiedImageFront());
            }
            if (oldReg.getIdentifiedImageBehind() != null){
                fileStorageService.deleteImage(oldReg.getIdentifiedImageBehind());
            }
        }

        log.info("Đang tiến hành lưu trữ file hình ảnh CCCD mới lên Server cho UserID: {}", userId);
        String imagePathFront = fileStorageService.saveImage(sellerRegistrationRequest.getIdentifiedImageFront(), "cccd");
        String imagePathBehind = fileStorageService.saveImage(sellerRegistrationRequest.getIdentifiedImageBehind(), "cccd");

        SellerRegistration sellerRegistration = oldRegistrationOpt.orElseGet(SellerRegistration::new);
        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.error("Không tìm thấy thông tin người dùng với ID: {} trong hệ thống.", userId);
            return new RuntimeException("User not found");
        });

        sellerRegistration.setUser1(user);
        sellerRegistration.setId(userId);
        sellerRegistration.setName(sellerRegistrationRequest.getName());
        sellerRegistration.setEmail(sellerRegistrationRequest.getEmail());
        sellerRegistration.setAddress(sellerRegistrationRequest.getAddress());
        sellerRegistration.setIdentityNumber(sellerRegistrationRequest.getIdentityNumber());
        sellerRegistration.setPhoneNumber(sellerRegistrationRequest.getPhoneNumber());
        sellerRegistration.setStatus(Status.PENDING.toString());
        sellerRegistration.setCreatedAt(LocalDateTime.now());
        sellerRegistration.setIdentifiedImageFront(imagePathFront);
        sellerRegistration.setIdentifiedImageBehind(imagePathBehind);

        sellerRegistrationRepository.save(sellerRegistration);
        log.info("Lưu đơn đăng ký quyền Seller của UserID: {} thành công, trạng thái: PENDING.", userId);

        return setDetail(sellerRegistration);
    }

    public List<SellerRegistrationResponse> getPendingRegistration(){
        log.info("Admin truy vấn danh sách hồ sơ đăng ký Seller đang chờ phê duyệt (PENDING).");
        List<SellerRegistration> pendingList = sellerRegistrationRepository.findByStatusOrderByCreatedAtDesc(Status.PENDING.toString());

        return pendingList.stream().map(registration -> new SellerRegistrationResponse(
                registration.getId(),
                registration.getName(),
                registration.getIdentityNumber(),
                registration.getPhoneNumber(),
                registration.getEmail(),
                registration.getAddress(),
                registration.getCreatedAt(),
                registration.getStatus(),
                registration.getIdentifiedImageFront(),
                registration.getIdentifiedImageBehind()
        )).collect(Collectors.toList());
    }

    @Transactional
    public void handleSellerRegistration(HandleSellerRegistrationRequest request){
        log.info("Admin thực hiện xử lý phê duyệt đơn đăng ký - Registration ID: {}, Hành động: {}", request.getRegistrationId(), request.getAdminAction());

        SellerRegistration sellerRegistration = sellerRegistrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> {
                    log.error("Không tìm thấy đơn đăng ký hợp lệ với mã số ID: {}", request.getRegistrationId());
                    return new RuntimeException("Registration not found");
                });

        User user = sellerRegistration.getUser1();
        String channel = "/topic/user-" + user.getId();

        if ("APPROVE".equalsIgnoreCase(request.getAdminAction())){
            sellerRegistration.setStatus(Status.APPROVED.toString());
            sellerRegistrationRepository.save(sellerRegistration);

            Roles sellerRole = roleRepository.findByRolename("SELLER");
            if (sellerRole == null ){
                log.error("Lỗi cấu hình hệ thống: Không tìm thấy thực thể phân quyền 'SELLER' trong Database.");
                throw new RuntimeException("Role seller not found");
            }
            user.getRoles().add(sellerRole);
            userRepository.save(user);
            log.info("Tài khoản UserID: {} chính thức được cấp quyền 'SELLER'.", user.getId());

            // Gửi thông báo WebSocket làm mới giao diện JavaFX phía client
            simpMessagingTemplate.convertAndSend(channel, "ROLE_UPDATED_TO_SELLER");
            log.info("Đã phát tín hiệu ROLE_UPDATED_TO_SELLER tới kênh: {}", channel);
        }
        else if ("REJECT".equalsIgnoreCase(request.getAdminAction())){
            sellerRegistration.setStatus(Status.REJECTED.toString());
            sellerRegistrationRepository.save(sellerRegistration);
            log.info("Đơn đăng ký Seller mã số {} bị từ chối bởi Admin.", request.getRegistrationId());

            // Gửi thông báo WebSocket làm mới giao diện JavaFX phía client
            simpMessagingTemplate.convertAndSend(channel, "REGISTRATION_REJECTED");
            log.info("Đã phát tín hiệu REGISTRATION_REJECTED tới kênh: {}", channel);
        }
    }

    public SellerRegistrationResponse getRegistrationDetailByUserId(Long userId){
        SellerRegistration sellerRegistration = sellerRegistrationRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Không tìm thấy hồ sơ đăng ký Seller nào liên kết với User ID: {}", userId);
                    return new RuntimeException("User not found");
                });
        return setDetail(sellerRegistration);
    }

    public SellerRegistrationResponse setDetail(SellerRegistration sellerRegistration){
        SellerRegistrationResponse sellerRegistrationResponse = new SellerRegistrationResponse();
        sellerRegistrationResponse.setId(sellerRegistration.getId());
        sellerRegistrationResponse.setAddress(sellerRegistration.getAddress());
        sellerRegistrationResponse.setEmail(sellerRegistration.getEmail());
        sellerRegistrationResponse.setName(sellerRegistration.getName());
        sellerRegistrationResponse.setPhoneNumber(sellerRegistration.getPhoneNumber());
        sellerRegistrationResponse.setIdentityNumber(sellerRegistration.getIdentityNumber());
        sellerRegistrationResponse.setIdentifiedImageFront(sellerRegistration.getIdentifiedImageFront());
        sellerRegistrationResponse.setIdentifiedImageBehind(sellerRegistration.getIdentifiedImageBehind());
        sellerRegistrationResponse.setStatus(sellerRegistration.getStatus());
        sellerRegistrationResponse.setCreatedAt(sellerRegistration.getCreatedAt());
        return sellerRegistrationResponse;
    }
}