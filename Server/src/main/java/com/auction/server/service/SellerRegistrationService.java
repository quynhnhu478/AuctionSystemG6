package com.auction.server.service;

import com.auction.server.exception.AuthException;
import com.auction.server.model.User.SellerRegistration;
import com.auction.server.model.User.Status;
import com.auction.server.payload.User.SellerRegistrationRequest;
import com.auction.server.payload.User.SellerRegistrationResponse;
import com.auction.server.repository.SellerRegistrationRepository;
import com.auction.server.util.FileStorageService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class SellerRegistrationService {
    private final SellerRegistrationRepository sellerRegistrationRepository;
    private final FileStorageService fileStorageService;

    public SellerRegistrationService(SellerRegistrationRepository sellerRegistrationRepository,
                                     FileStorageService fileStorageService){
        this.sellerRegistrationRepository = sellerRegistrationRepository;
        this.fileStorageService = fileStorageService;
    }

    public SellerRegistrationResponse registerAsSeller(Long userId,SellerRegistrationRequest sellerRegistrationRequest){
        boolean hasPendingOrder = sellerRegistrationRepository.existsByUser1_IdAndStatus(userId, Status.PENDING.toString());
        if (hasPendingOrder){
            throw new AuthException("Your registration is pending admin approval!");
        }
        boolean isAlreadySeller = sellerRegistrationRepository.existsByUser1_IdAndStatus(userId, Status.APPROVED.toString());
        if (isAlreadySeller){
            throw new AuthException("Your account is already registered as a seller!");
        }
        String imagePathFront = fileStorageService.saveImage(sellerRegistrationRequest.getIdentifiedImageFront(), "cccd");
        String imagePathBehind = fileStorageService.saveImage(sellerRegistrationRequest.getIdentifiedImageBehind(), "cccd");

        SellerRegistration sellerRegistration = new SellerRegistration();
        sellerRegistration.setId(userId);
        sellerRegistration.setName(sellerRegistrationRequest.getName());
        sellerRegistration.setEmail(sellerRegistrationRequest.getEmail());
        sellerRegistration.setAddress(sellerRegistrationRequest.getAddress());
        sellerRegistration.setIdentityNumber(sellerRegistrationRequest.getIdentityNumber());
        sellerRegistration.setPhoneNumber(sellerRegistrationRequest.getPhoneNumber());
        sellerRegistration.setStatus(Status.PENDING.toString());
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
}
