package com.sanjay.bms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//public record AccountDto(Long id,
//                         String accountNumber,
//                         String maskedAccountNumber,
//                         String accountHolderName,
//                         String accountType,
//                         BigDecimal balance,
//                         String accountStatus,
//                         LocalDateTime createdAt,
//                         LocalDateTime lastTransactionDate,
//                         BigDecimal dailyTransactionLimit,
//                         BigDecimal perTransactionLimit,
//                         BigDecimal interestRate,
//                         BigDecimal minimumBalance,
//                         String userId
//){}


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
    private String userId;
}

