package com.auction.server.model.user;

import com.auction.server.model.BaseEntity;
import com.auction.server.model.item.Item;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name ="user")
public class User extends BaseEntity {

    @Column
    private String name;
    @Column
    private String email;
    @Column
    private String password;
    @Column
    private double balance;
    @ManyToMany
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "id_user"),
            inverseJoinColumns = @JoinColumn(name = "id_role")
    )
    private Set<Roles> roles = new HashSet<>();
    public User(){}

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL)
    private final Set<Item>  items = new HashSet<>();

    @OneToOne(mappedBy = "user1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SellerRegistration sellerRegistration;

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
    public void setRoles(Set<Roles> roles){
        this.roles = roles;
    }
    public Set<Roles> getRoles(){
        return roles;
    }
    public void setSellerRegistration(SellerRegistration sellerRegistration){
        this.sellerRegistration = sellerRegistration;
    }
    public SellerRegistration getSellerRegistration(){
        return sellerRegistration;
    }
    public  void setBalance(double balance){
        this.balance = balance;
    }
    public double getBalance(){
        return balance;
    }
}
