package com.auction.common.payload;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String message;

    private Set<String> roles;
    private String sellerStatus;
    private double balance;
    public UserResponse(){}
    public UserResponse(Long id, String name, String email, String message){
        this.id = id;
        this.name = name;
        this.email = email;
        this.message = message;
    }
    public void setId(Long id){
        this.id = id;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setEmail(String email){
        this.email =email;
    }
    public void setMessage(String message){
        this.message = message;
    }
    public Long getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public String getEmail(){
        return email;
    }
    public String getMessage(){
        return message;
    }
    public Set<String> getRoles(){
        return roles;
    }
    public void setRoles(Set<String> roles){
        this.roles = roles;
    }
    public String getSellerStatus(){
        return sellerStatus;
    }
    public void setSellerStatus(String sellerStatus){
        this.sellerStatus = sellerStatus;
    }
    public double getBalance(){
        return balance;
    }
    public void setBalance(double balance){
        this.balance = balance;
    }

}
