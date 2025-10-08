package com.sanjay.bms.dto;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionApprovalRequest {
    private Long transactionId;
    private Boolean approved;
    private String remarks;
}