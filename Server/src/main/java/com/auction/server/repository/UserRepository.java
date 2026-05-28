package com.auction.server.repository;

import com.auction.server.model.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.name = :name AND u.password = :password")
    User findByNameAndPassword(@Param("name") String name, @Param("password") String password);
    @EntityGraph(attributePaths = {"roles"})
    User findByName(String name);
    @EntityGraph(attributePaths = {"roles", "sellerRegistration"})
    List<User> findAll();

}
