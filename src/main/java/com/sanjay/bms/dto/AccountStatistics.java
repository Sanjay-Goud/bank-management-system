package com.sanjay.bms.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountStatistics {
    private Long totalAccounts;
    private BigDecimal totalBalance;
    private BigDecimal averageBalance;
    private BigDecimal maxBalance;
    private BigDecimal minBalance;
    private Long activeAccounts;
    private Long inactiveAccounts;
    private Long frozenAccounts;
    private Map<String, Long> accountTypeDistribution;
    private Map<String, BigDecimal> balanceByType;
}