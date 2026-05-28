package com.auction.server.repository;

import com.auction.server.model.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.name = :name AND u.password = :password")
    User findByNameAndPassword(@Param("name") String name, @Param("password") String password);
    @EntityGraph(attributePaths = {"roles"})
    User findByName(String name);
    @EntityGraph(attributePaths = {"roles", "sellerRegistration"})
    List<User> findAll();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findWithLockById(@Param("id") Long id);
}
