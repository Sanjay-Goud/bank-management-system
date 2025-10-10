package com.sanjay.bms.dto;

import lombok.*;

@Setter
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionApprovalRequest {
    private Long transactionId;
    private Boolean approved;
    private String remarks;
}