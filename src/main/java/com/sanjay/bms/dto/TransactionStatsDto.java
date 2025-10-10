package com.sanjay.bms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionStatsDto {
    private Long totalTransactions;
    private BigDecimal totalAmount;
    private Long depositsCount;
    private BigDecimal depositsTotal;
    private Long withdrawalsCount;
    private BigDecimal withdrawalsTotal;
    private Long transfersCount;
    private BigDecimal transfersTotal;
    private BigDecimal averageTransactionAmount;
    private BigDecimal largestTransaction;
    private BigDecimal smallestTransaction;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal netAmount;
    private Long totalCount;
}