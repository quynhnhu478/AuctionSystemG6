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
    @Column
    private LocalDateTime createdAt;

    @Setter
    @Getter
    @Column
    private String status;

    @Setter
    @Getter
    @Column
    private String identifiedImage;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user1;

    public SellerRegistration(){}
    public SellerRegistration(String name, String identityNumber, String phoneNumber,
                              String email, String address, LocalDateTime createdAt,
                              String status, String  identifiedImage, User user1){
        this.name =name;
        this.identityNumber = identityNumber;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address =address;
        this.createdAt = createdAt;
        this.status = status;
        this.identifiedImage = identifiedImage;
        this.user1 = user1;
    }

}
