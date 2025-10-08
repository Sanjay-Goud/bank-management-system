package com.sanjay.bms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DailySummaryDto {
    private LocalDateTime date;
    private Long totalTransactions;
    private BigDecimal totalTransactionAmount;
    private Long depositsCount;
    private BigDecimal depositsAmount;
    private Long withdrawalsCount;
    private BigDecimal withdrawalsAmount;
    private Long transfersCount;
    private BigDecimal transfersAmount;
    private Long newAccountsToday;
    private Long newUsersToday;
    private Long activeUsers;
    private BigDecimal systemBalance;
}
