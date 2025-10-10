package com.sanjay.bms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Data
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String role; // ADMIN, USER

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

    @Column(nullable = false)
    private Boolean enabled = true;

    // New fields for enhanced security
    private String phoneNumber;

    private String twoFactorSecret; // For 2FA

    @Column(nullable = false)
    private Boolean twoFactorEnabled = false;

    private String address;

    private String dateOfBirth;

    // Security fields
    private Integer failedLoginAttempts = 0;

    private LocalDateTime accountLockedUntil;

    private String lastLoginIp;

    @Column(nullable = false)
    private Boolean accountLocked = false;
}