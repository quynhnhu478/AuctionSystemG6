package com.auction.server.repository;

import com.auction.server.model.User.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByNameAndPassword(String name, String password);
    User findByName(String name);
}
