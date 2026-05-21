package com.auction.server.model.User;

import com.auction.server.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name ="seller_registrations")
public class SellerRegistration extends BaseEntity {
    @Setter
    @Getter
    @Column
    private String name;

    @Setter
    @Getter
    @Column
    private String identityNumber;

    @Setter
    @Getter
    @Column
    private String phoneNumber;

    @Setter
    @Getter
    @Column
    private String email;

    @Setter
    @Getter
    @Column
    private String address;

    @Setter
    @Getter
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Setter
    @Getter
    @Column
    private String status;

    @Setter
    @Getter
    @Column
    private String identifiedImageFront;

    @Setter
    @Getter
    @Column
    private String identifiedImageBehind;

    @Setter
    @Getter
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user1;

    public SellerRegistration(){}
    public SellerRegistration(String name, String identityNumber, String phoneNumber,
                              String email, String address, LocalDateTime createdAt,
                              String status, String  identifiedImageFront,
                              String  identifiedImageBehind,User user1){
        this.name =name;
        this.identityNumber = identityNumber;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address =address;
        this.createdAt = createdAt;
        this.status = status;
        this.identifiedImageFront = identifiedImageFront;
        this.identifiedImageBehind = identifiedImageBehind;
        this.user1 = user1;

    }
    @PrePersist
    public void onCreate(){
        this.createdAt = LocalDateTime.now();
    }

}
