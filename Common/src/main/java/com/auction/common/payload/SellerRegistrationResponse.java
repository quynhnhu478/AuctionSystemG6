package com.auction.common.payload;



import java.time.LocalDateTime;

public class SellerRegistrationResponse {

    private Long id;

    private String name;


    private String identityNumber;

    private String phoneNumber;

    private String email;

    private String address;

    private LocalDateTime createdAt;

    private String status;

    private String identifiedImageFront;

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
    public void setId(Long id){
        this.id = id;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setIdentityNumber(String identityNumber){
        this.identityNumber = identityNumber;
    }
    public void setPhoneNumber(String phoneNumber){
        this.phoneNumber = phoneNumber;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public void setAddress(String address){
        this.address = address;
    }
    public void setCreatedAt(LocalDateTime createdAt){
        this.createdAt = createdAt;
    }
    public void setStatus(String status){
        this.status = status;
    }
    public void setIdentifiedImageFront(String identifiedImageFront){
        this.identifiedImageFront = identifiedImageFront;
    }
    public void setIdentifiedImageBehind(String identifiedImageBehind){
        this.identifiedImageBehind = identifiedImageBehind;
    }
    public Long getId() {
        return id;
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
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public String getStatus() {
        return status;
    }
    public String getIdentifiedImageFront() {
        return identifiedImageFront;
    }
    public String getIdentifiedImageBehind() {
        return identifiedImageBehind;
    }

}
