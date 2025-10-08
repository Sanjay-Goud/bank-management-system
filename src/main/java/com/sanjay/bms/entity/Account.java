package com.sanjay.bms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String accountNumber;

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false)
    private String accountType; // SAVINGS, CHECKING, FIXED_DEPOSIT

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private String accountStatus; // Active, Inactive, Frozen, Closed

    // New fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastTransactionDate;

    // Transaction limits
    @Column(nullable = false)
    private BigDecimal dailyTransactionLimit = new BigDecimal("100000");

    @Column(nullable = false)
    private BigDecimal perTransactionLimit = new BigDecimal("50000");

    private BigDecimal dailyTransactionTotal = BigDecimal.ZERO;

    private LocalDateTime dailyLimitResetDate;

    // Interest rate for savings accounts
    private BigDecimal interestRate;

    // Minimum balance requirement
    private BigDecimal minimumBalance = BigDecimal.ZERO;

    // Account closure
    private LocalDateTime closedAt;

    private String closureReason;

    // For admin actions
    private String frozenReason;

    private LocalDateTime frozenAt;
}