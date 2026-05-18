package com.auction.server.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;

@Entity
@Table(name = "role")
public class Roles extends BaseEntity{
    @Setter
    @Getter
    private String rolename;

    public String getRoleName(){
        return rolename;
    }


}
