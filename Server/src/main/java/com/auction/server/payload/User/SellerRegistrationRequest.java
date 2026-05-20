package com.auction.server.payload.User;

import com.auction.server.model.User.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class SellerRegistrationRequest {

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
    private String identifiedImageFront;

    @Setter
    @Getter
    private String identifiedImageBehind;

    public SellerRegistrationRequest(){}
    public SellerRegistrationRequest(String name, String identityNumber, String phoneNumber,
                              String email, String address,  String  identifiedImageFront,
                                     String  identifiedImageBehind){
        this.name =name;
        this.identityNumber = identityNumber;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address =address;
        this.identifiedImageFront = identifiedImageFront;
        this.identifiedImageBehind = identifiedImageBehind;
    }
}
