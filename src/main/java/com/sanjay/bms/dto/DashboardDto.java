package com.sanjay.bms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDto {
    private BigDecimal totalBalance;
    private Integer totalAccounts;
    private Integer activeAccounts;
    private Long todayTransactions;
    private List<TransactionDto> recentTransactions;
    private List<AccountDto> accounts;
    private List<NotificationDto> unreadNotifications;
}