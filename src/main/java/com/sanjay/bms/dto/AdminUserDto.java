package com.sanjay.bms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String role;
    private Boolean enabled;
    private Boolean accountLocked;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private Integer totalAccounts;
    private BigDecimal totalBalance;
}