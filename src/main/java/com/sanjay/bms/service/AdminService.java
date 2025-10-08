package com.sanjay.bms.service;

import com.sanjay.bms.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AdminService {
    AdminDashboardDto getDashboard();
    List<AdminUserDto> getAllUsers();
    AdminUserDto getUserById(Long userId);
    void toggleUserStatus(Long userId, HttpServletRequest request);
    void lockUser(Long userId, String reason, HttpServletRequest request);
    void unlockUser(Long userId, HttpServletRequest request);
    void freezeAccount(FreezeAccountRequest request, HttpServletRequest httpRequest);
    void unfreezeAccount(Long accountId, HttpServletRequest request);
    void closeAccount(CloseAccountRequest request, HttpServletRequest httpRequest);
//    List<AuditLogDto> getAuditLogs(int limit);
    List<AuditLogDto> getAuditLogsByUser(String username);
    List<AuditLogDto> getCriticalLogs();
    List<TransactionDto> getHighValueTransactions(int limit);
    DailySummaryDto getDailySummary();
    TransactionStatsDto getTransactionStats(LocalDateTime startDate, LocalDateTime endDate);

    List<AccountDto> getAllAccounts();

    void freezeAccount(Long accountId, String reason, HttpServletRequest httpRequest);

    List<TransactionDto> getAllTransactions();

    void closeAccount(Long accountId, String reason, HttpServletRequest httpRequest);

    void approveTransaction(Long transactionId, Boolean approved, String remarks, HttpServletRequest httpRequest);

    List<TransactionDto> getPendingTransactions();

    AdminDashboardDto getAdminDashboard();

    Map<String, Long> getAccountTypeDistribution();

    Map<String, Object> getTransactionVolume(String period);

    AdminUserDto getUserDetails(Long userId);

    void enableUser(Long userId, HttpServletRequest request);

    void disableUser(Long userId, HttpServletRequest request);

    List<AuditLogDto> getAuditLogs(String username, String severity, int limit);


}