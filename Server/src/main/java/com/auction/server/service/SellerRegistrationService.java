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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class SellerRegistrationService {
    private final SellerRegistrationRepository sellerRegistrationRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private SimpMessagingTemplate simpMessagingTemplate;


    public SellerRegistrationService(SellerRegistrationRepository sellerRegistrationRepository,
                                     FileStorageService fileStorageService, UserRepository userRepository,
                                        RoleRepository roleRepository, SimpMessagingTemplate simpMessagingTemplate ) {
        this.sellerRegistrationRepository = sellerRegistrationRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;

    }

    public SellerRegistrationResponse registerAsSeller(Long userId,SellerRegistrationRequest sellerRegistrationRequest){

        Optional<SellerRegistration> oldRegistrationOpt = sellerRegistrationRepository.findById(userId);
        if (oldRegistrationOpt.isPresent()){
            SellerRegistration oldRegistration = oldRegistrationOpt.get();

            if(Status.PENDING.toString().equals(oldRegistration.getStatus())){
                throw new AuthException("Your registration is pending admin approval!");
            }
            else if(Status.APPROVED.toString().equals(oldRegistration.getStatus())){
                throw new AuthException("Your account is already registered as a seller!");
            }
        }
        // Xử lý file ảnh khi đơn bị reject
        if (oldRegistrationOpt.isPresent()){
            SellerRegistration oldReg = oldRegistrationOpt.get();

            if (oldReg.getIdentifiedImageFront() != null){
                fileStorageService.deleteImage(oldReg.getIdentifiedImageFront());
            }
            if (oldReg.getIdentifiedImageBehind() != null){
                fileStorageService.deleteImage(oldReg.getIdentifiedImageBehind());
            }
        }


        String imagePathFront = fileStorageService.saveImage(sellerRegistrationRequest.getIdentifiedImageFront(), "cccd");
        String imagePathBehind = fileStorageService.saveImage(sellerRegistrationRequest.getIdentifiedImageBehind(), "cccd");



        SellerRegistration sellerRegistration = oldRegistrationOpt.orElseGet(SellerRegistration::new);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
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

    public List<SellerRegistrationResponse> getPendingRegistration(){
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
        SellerRegistration sellerRegistration = sellerRegistrationRepository.findById(request.getRegistrationId())
                .orElseThrow(() -> new RuntimeException("Registration not found"));

        User user = sellerRegistration.getUser1();
        if ("APPROVE".equalsIgnoreCase(request.getAdminAction())){
            sellerRegistration.setStatus(Status.APPROVED.toString());
            sellerRegistrationRepository.save(sellerRegistration);
            Roles sellerRole = roleRepository.findByRolename("SELLER");
            if (sellerRole == null ){
                throw new RuntimeException("Role seller not found");
            }
            user.getRoles().add(sellerRole);
            userRepository.save(user);

            String channel = "/topic/user-" +user.getId();
            simpMessagingTemplate.convertAndSend(channel, "ROLE_UPDATED_TO_SELLER");
        }
        else if ("REJECT".equalsIgnoreCase(request.getAdminAction())){
            sellerRegistration.setStatus(Status.REJECTED.toString());
            sellerRegistrationRepository.save(sellerRegistration);

            String channel = "/topic/user-" +user.getId();
            simpMessagingTemplate.convertAndSend(channel, "REGISTRATION_REJECTED");
        }
    }
}
