package com.sanjay.bms.repository;

import com.sanjay.bms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    List<User> findByRole(String role);
    List<User> findByEnabled(Boolean enabled);

    @Query("SELECT u FROM User u WHERE u.accountLocked = true")
    List<User> findLockedAccounts();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    Long countNewUsersSince(@Param("startDate") LocalDateTime startDate);
}