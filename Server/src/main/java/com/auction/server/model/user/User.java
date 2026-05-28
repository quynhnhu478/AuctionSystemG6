package com.auction.server.model.user;

import com.auction.server.model.Auction;
import com.auction.server.model.AutoBid;
import com.auction.server.model.BaseEntity;
import com.auction.server.model.BidHistory;
import com.auction.server.model.item.Item;
import jakarta.persistence.*;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "user")
public class User extends BaseEntity {

    @Setter
    @Column
    private String name;

    @Setter
    @Column
    private String email;

    @Setter
    @Column
    private String password;

    @Setter
    @Column
    private double balance;

    @Setter
    @Column(name = "freeze_balance", columnDefinition = "double default 0.0")
    private double freeze_balance = 0;

    @Setter
    @ManyToMany(fetch = FetchType.EAGER) // Chuyển sang EAGER để khi lấy User luôn có sẵn danh sách Roles, tránh lỗi LazyInitializationException ở tầng bảo mật/Service
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "id_user"),
            inverseJoinColumns = @JoinColumn(name = "id_role")
    )
    private Set<Roles> roles = new HashSet<>();

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final Set<Item> items = new HashSet<>();

    @Setter
    @OneToOne(mappedBy = "user1", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SellerRegistration sellerRegistration;

    // Đổi tên hàm getAuctions thành setCreatedAuctions để đồng bộ và chuẩn hóa quy tắc đặt tên Java Bean
    // 1. Danh sách các phiên đấu giá mà User này ĐĂNG BÁN
    @Setter
    @OneToMany(mappedBy = "seller", fetch = FetchType.LAZY)
    private List<Auction> createdAuctions = new ArrayList<>();

    // 2. Danh sách các phiên đấu giá mà User này ĐÃ THẮNG
    @Setter
    @OneToMany(mappedBy = "winner", fetch = FetchType.LAZY)
    private List<Auction> wonAuctions = new ArrayList<>();

    // 3. Danh sách các lượt đặt giá mà User này ĐÃ THỰC HIỆN
    @Setter
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<BidHistory> bidHistories = new ArrayList<>();

    // 4. Danh sách các cấu hình tự động đấu giá mà User này ĐÃ CÀI ĐẶT
    @Setter
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<AutoBid> autoBids = new ArrayList<>();

    // Constructor mặc định bắt buộc cho JPA Provider (Hibernate)
    public User(){}

    public User(String name, String email, String password){
        this.name = name;
        this.email = email;
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

    public Long getID(){
        return id;
    }

    public void setID(Long id){
        this.id = id;
    }

    public Set<Roles> getRoles(){
        return roles;
    }

    public SellerRegistration getSellerRegistration(){
        return sellerRegistration;
    }

    public double getBalance(){
        return balance;
    }

    public double getFreeze_balance(){
        return freeze_balance;
    }

    public Set<Item> getItems(){
        return items;
    }

    public List<Auction> getCreatedAuctions(){
        return createdAuctions;
    }

    public List<Auction> getWonAuctions(){
        return wonAuctions;
    }

    public List<BidHistory> getBidHistories() {
        return bidHistories;
    }

    public List<AutoBid> getAutoBids() {
        return autoBids;
    }

}