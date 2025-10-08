package com.sanjay.bms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardDto {
    private Long totalUsers;
    private Long totalAccounts;
    private Long activeAccounts;
    private Long frozenAccounts;
    private BigDecimal totalSystemBalance;
    private Long todayTransactions;
    private BigDecimal todayTransactionVolume;
    private Long pendingApprovals;
    private Long criticalAlerts;
    private List<TransactionDto> recentHighValueTransactions;
    private List<AuditLogDto> recentCriticalLogs;
}
