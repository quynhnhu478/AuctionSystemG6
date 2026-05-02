package com.auction.server.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;

@Entity
@Table(name = "role")
public class Roles extends BaseEntity{
    private String rolename;

    public String getRoleName(){
        return rolename;
    }


}
