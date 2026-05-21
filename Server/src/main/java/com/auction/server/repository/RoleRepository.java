package com.auction.server.repository;


import com.auction.server.model.User.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Roles,Long> {
    Roles findByRolename(String rolename);
}
