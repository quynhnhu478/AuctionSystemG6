package com.auction.server.payload.User;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class SellerRegistrationResponse {
    @Setter
    @Getter
    private Long id;

    @Setter
    @Getter
    private String name;

    @Setter
    @Getter
    private String identityNumber;

    @Setter
    @Getter
    private String phoneNumber;

    @Setter
    @Getter
    private String email;

    @Setter
    @Getter
    private String address;

    @Setter
    @Getter
    private LocalDateTime createdAt;

    @Setter
    @Getter
    private String status;

    @Setter
    @Getter
    private String identifiedImageFront;

    @Setter
    @Getter
    private String identifiedImageBehind;

    public SellerRegistrationResponse(){}
    public SellerRegistrationResponse(Long id, String name, String identityNumber, String phoneNumber,
                                     String email, String address, LocalDateTime createdAt,
                                     String status, String  identifiedImageFront,
                                      String  identifiedImageBehind){
        this.id = id;
        this.name =name;
        this.identityNumber = identityNumber;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address =address;
        this.createdAt = createdAt;
        this.status = status;
        this.identifiedImageFront = identifiedImageFront;
        this.identifiedImageBehind = identifiedImageBehind;
    }
}
