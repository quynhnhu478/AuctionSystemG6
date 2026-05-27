package com.auction.server.model.user;

import com.auction.server.model.Auction;
import com.auction.server.model.AutoBid;
import com.auction.server.model.BaseEntity;
import com.auction.server.model.BidHistory;
import com.auction.server.model.item.Item;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.List;
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
    @Column(columnDefinition = "double default 0.0")
    private double freeze_balance = 0;
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

    // 1. Danh sách các phiên đấu giá mà User này ĐĂNG BÁN
    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    private List<Auction> createdAuctions;

    // 2. Danh sách các phiên đấu giá mà User này ĐÃ THẮNG
    @OneToMany(mappedBy = "winner", fetch = FetchType.LAZY)
    private List<Auction> wonAuctions;

    // 3. Danh sách các lượt đặt giá mà User này ĐÃ THỰC HIỆN
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<BidHistory> bidHistories;

    // 4. Danh sách các cấu hình tự động đấu giá mà User này ĐÃ CÀI ĐẶT
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<AutoBid> autoBids;

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
    public void setFreeze_balance(double freeze_balance){
        this.freeze_balance = freeze_balance;
    }
    public double getFreeze_balance(){
        return freeze_balance;
    }
    public Set<Item> getItems(){
        return items;
    }
    public void getAuctions(List<Auction> auctions){
        this.createdAuctions = auctions;
    }
    public List<Auction> getCreatedAuctions(){
        return createdAuctions;
    }
    public void setWonAuctions(List<Auction> wonAuctions){
        this.wonAuctions = wonAuctions;
    }
    public List<Auction> getWonAuctions(){
        return wonAuctions;
    }

}
