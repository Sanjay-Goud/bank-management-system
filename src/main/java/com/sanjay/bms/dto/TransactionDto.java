package com.sanjay.bms.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDto {
    private Long id;
    private String transactionType;
    private Long accountId;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String description;
    private LocalDateTime transactionDate;
    private String referenceNumber;
    private Long toAccountId;
    private String status;
}