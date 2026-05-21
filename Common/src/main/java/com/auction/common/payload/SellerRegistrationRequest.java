package com.auction.common.payload;

public class SellerRegistrationRequest {

    private String name;

    private String identityNumber;

    private String phoneNumber;

    private String email;

    private String address;

    private String identifiedImageFront;

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
    public void setName(String name) {
        this.name = name;
    }
    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;

    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public void setIdentifiedImageFront(String identifiedImageFront) {
        this.identifiedImageFront = identifiedImageFront;
    }
    public void setIdentifiedImageBehind(String identifiedImageBehind) {
        this.identifiedImageBehind = identifiedImageBehind;
    }
    public String getName() {
        return name;
    }
    public String getIdentityNumber() {
        return identityNumber;

    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public String getEmail() {
        return email;

    }
    public String getAddress() {
        return address;
    }
    public String getIdentifiedImageFront() {
        return identifiedImageFront;
    }
    public String getIdentifiedImageBehind() {
        return identifiedImageBehind;
    }

}
