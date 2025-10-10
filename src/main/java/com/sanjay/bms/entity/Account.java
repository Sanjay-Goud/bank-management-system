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
    private String accountType;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private String accountStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastTransactionDate;

    @Column(nullable = false)
    private BigDecimal dailyTransactionLimit = new BigDecimal("100000");

    @Column(nullable = false)
    private BigDecimal perTransactionLimit = new BigDecimal("50000");

    private BigDecimal dailyTransactionTotal = BigDecimal.ZERO;
    private LocalDateTime dailyLimitResetDate;
    private BigDecimal interestRate;
    private BigDecimal minimumBalance = BigDecimal.ZERO;
    private LocalDateTime closedAt;
    private String closureReason;
    private String frozenReason;
    private LocalDateTime frozenAt;

    // FIXED: Added helper method to get user ID
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
}