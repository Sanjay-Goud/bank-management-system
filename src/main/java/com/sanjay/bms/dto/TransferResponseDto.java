package com.sanjay.bms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponseDto {
    private Long transactionId;
    private String referenceNumber;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String status;
    private LocalDateTime transactionDate;
    private String message;
    private BigDecimal fromAccountBalance;
    private BigDecimal toAccountBalance;
}
