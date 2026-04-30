package com.auction.server.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name =" users")
public class User extends BaseEntity {

    private String name;
    private String email;
    private String password;
    public User(){}

    public User(String name, String email, String password){
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public void setName(String name){
        this.name = name;
    }
    public void setEmail(String email){
        this.email = email;
    }
    public void setPassword(String password){
        this.password = password;
    }
    public String getName(){
        return name;
    }
    public String getEmail(){
        return email;
    }
    public String getPassword(){
        return password;
    }
    public void setID(Long id){
        this.id = id;
    }
    public Long getID(){
        return id;
    }
}
