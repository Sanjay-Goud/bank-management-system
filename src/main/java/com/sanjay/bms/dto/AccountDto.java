package com.sanjay.bms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {
    private Long id;
    private String accountNumber;
    private String maskedAccountNumber; // For public display
    private String accountHolderName;
    private String accountType;
    private BigDecimal balance;
    private String accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime lastTransactionDate;
    private BigDecimal dailyTransactionLimit;
    private BigDecimal perTransactionLimit;
    private BigDecimal interestRate;
    private BigDecimal minimumBalance;
    private Long userId;  // ✅ Changed from String to Long
}