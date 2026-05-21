package com.auction.server.model.user;

import com.auction.server.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "role")
public class Roles extends BaseEntity {
    @Setter
    @Getter
    private String rolename;

    public String getRoleName(){
        return rolename;
    }
    public void setRoleName(String rolename){
        this.rolename = rolename;
    }


}
