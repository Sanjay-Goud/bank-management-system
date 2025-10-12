package com.sanjay.bms.service.impl;

import com.sanjay.bms.dto.*;
import com.sanjay.bms.entity.*;
import com.sanjay.bms.exception.ResourceNotFoundException;
import com.sanjay.bms.mapper.*;
import com.sanjay.bms.repository.*;
import com.sanjay.bms.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AccountService accountService;

    @Override
    public AdminDashboardDto getDashboard() {
        AdminDashboardDto dashboard = new AdminDashboardDto();

        dashboard.setTotalUsers(userRepository.count());
        dashboard.setTotalAccounts(accountRepository.count());
        dashboard.setActiveAccounts(accountRepository.countByAccountStatus("Active"));
        dashboard.setFrozenAccounts(accountRepository.countByAccountStatus("Frozen"));

        BigDecimal totalBalance = accountRepository.findAll().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTotalSystemBalance(totalBalance);

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        dashboard.setTodayTransactions(transactionRepository.countTransactionsSince(startOfDay));

        BigDecimal todayVolume = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionDate().isAfter(startOfDay))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTodayTransactionVolume(todayVolume);

        dashboard.setPendingApprovals(0L);
        dashboard.setCriticalAlerts(auditLogRepository.findBySeverityOrderByTimestampDesc("CRITICAL").size() > 0 ?
                (long) auditLogRepository.findBySeverityOrderByTimestampDesc("CRITICAL").size() : 0L);

        List<TransactionDto> highValueTxns = transactionRepository.findAll().stream()
                .filter(t -> t.getAmount().compareTo(new BigDecimal("50000")) > 0)
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .limit(10)
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
        dashboard.setRecentHighValueTransactions(highValueTxns);

        List<AuditLogDto> criticalLogs = auditLogRepository.findBySeverityOrderByTimestampDesc("CRITICAL")
                .stream()
                .limit(10)
                .map(AuditLogMapper::mapToAuditLogDto)
                .collect(Collectors.toList());
        dashboard.setRecentCriticalLogs(criticalLogs);

        return dashboard;
    }

    @Override
    public List<AdminUserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToAdminUserDto)
                .collect(Collectors.toList());
    }

    @Override
    public AdminUserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToAdminUserDto(user);
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId, HttpServletRequest request) {
        log.info("🔵 START: Toggling user status for ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            user.setEnabled(!user.getEnabled());
            userRepository.save(user);

            String action = user.getEnabled() ? "USER_ENABLED" : "USER_DISABLED";
            auditService.logAction("admin", action,
                    "User " + user.getUsername() + " status changed", request, "INFO");

            log.info("🟢 SUCCESS: User {} status toggled to: {}", user.getUsername(), user.getEnabled());

        } catch (Exception e) {
            log.error("🔴 ERROR in toggleUserStatus: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void lockUser(Long userId, String reason, HttpServletRequest request) {
        log.info("🔵 START: Locking user ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            user.setAccountLocked(true);
            user.setAccountLockedUntil(LocalDateTime.now().plusDays(30));
            userRepository.save(user);

            notificationService.createNotification(user, "Account Locked",
                    "Your account has been locked. Reason: " + reason, "SECURITY");

            auditService.logAction("admin", "USER_LOCKED",
                    "User " + user.getUsername() + " locked. Reason: " + reason, request, "CRITICAL");

            log.info("🟢 SUCCESS: User {} locked", user.getUsername());

        } catch (Exception e) {
            log.error("🔴 ERROR in lockUser: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void unlockUser(Long userId, HttpServletRequest request) {
        log.info("🔵 START: Unlocking user ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            user.setAccountLocked(false);
            user.setAccountLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            notificationService.createNotification(user, "Account Unlocked",
                    "Your account has been unlocked.", "SECURITY");

            auditService.logAction("admin", "USER_UNLOCKED",
                    "User " + user.getUsername() + " unlocked", request, "INFO");

            log.info("🟢 SUCCESS: User {} unlocked", user.getUsername());

        } catch (Exception e) {
            log.error("🔴 ERROR in unlockUser: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ✅ FIXED: Proper error handling for freeze/unfreeze/close operations
    @Override
    @Transactional
    public void freezeAccount(FreezeAccountRequest request, HttpServletRequest httpRequest) {
        log.info("🔵 START: Admin freeze account request - ID: {}", request.getAccountId());

        try {
            accountService.freezeAccount(request.getAccountId(), request.getReason(),
                    "admin", httpRequest);
            log.info("🟢 SUCCESS: Account frozen via admin");

        } catch (Exception e) {
            log.error("🔴 ERROR in admin freezeAccount: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void unfreezeAccount(Long accountId, HttpServletRequest request) {
        log.info("🔵 START: Admin unfreeze account - ID: {}", accountId);

        try {
            accountService.unfreezeAccount(accountId, "admin", request);
            log.info("🟢 SUCCESS: Account unfrozen via admin");

        } catch (Exception e) {
            log.error("🔴 ERROR in admin unfreezeAccount: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void closeAccount(CloseAccountRequest request, HttpServletRequest httpRequest) {
        log.info("🔵 START: Admin close account request - ID: {}", request.getAccountId());

        try {
            accountService.closeAccount(request.getAccountId(), request.getReason(),
                    "admin", httpRequest);
            log.info("🟢 SUCCESS: Account closed via admin");

        } catch (Exception e) {
            log.error("🔴 ERROR in admin closeAccount: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<AuditLogDto> getAuditLogs(String username, String severity, int limit) {
        List<AuditLog> logs;

        if (username != null && severity != null) {
            logs = auditLogRepository.findByUsernameAndSeverityOrderByTimestampDesc(username, severity);
        } else if (username != null) {
            logs = auditLogRepository.findByUsernameOrderByTimestampDesc(username);
        } else if (severity != null) {
            logs = auditLogRepository.findBySeverityOrderByTimestampDesc(severity);
        } else {
            logs = auditLogRepository.findTopNByOrderByTimestampDesc(limit);
        }

        if (logs.size() > limit) {
            logs = logs.subList(0, limit);
        }

        return logs.stream()
                .map(AuditLogMapper::mapToAuditLogDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLogDto> getAuditLogsByUser(String username) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username).stream()
                .map(AuditLogMapper::mapToAuditLogDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditLogDto> getCriticalLogs() {
        return auditLogRepository.findBySeverityOrderByTimestampDesc("CRITICAL").stream()
                .map(AuditLogMapper::mapToAuditLogDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionDto> getHighValueTransactions(int limit) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getAmount().compareTo(new BigDecimal("50000")) > 0)
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .limit(limit)
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    public DailySummaryDto getDailySummary() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        List<Transaction> todayTxns = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionDate().isAfter(startOfDay))
                .collect(Collectors.toList());

        DailySummaryDto summary = new DailySummaryDto();
        summary.setDate(LocalDateTime.now());
        summary.setTotalTransactions((long) todayTxns.size());

        BigDecimal totalAmount = todayTxns.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setTotalTransactionAmount(totalAmount);

        List<Transaction> deposits = todayTxns.stream()
                .filter(t -> "DEPOSIT".equals(t.getTransactionType()))
                .collect(Collectors.toList());
        summary.setDepositsCount((long) deposits.size());
        summary.setDepositsAmount(deposits.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        List<Transaction> withdrawals = todayTxns.stream()
                .filter(t -> "WITHDRAW".equals(t.getTransactionType()))
                .collect(Collectors.toList());
        summary.setWithdrawalsCount((long) withdrawals.size());
        summary.setWithdrawalsAmount(withdrawals.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        List<Transaction> transfers = todayTxns.stream()
                .filter(t -> t.getTransactionType().contains("TRANSFER"))
                .collect(Collectors.toList());
        summary.setTransfersCount((long) transfers.size() / 2);
        summary.setTransfersAmount(transfers.stream()
                .filter(t -> "TRANSFER_OUT".equals(t.getTransactionType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        summary.setNewAccountsToday(accountRepository.findAll().stream()
                .filter(a -> a.getCreatedAt().isAfter(startOfDay))
                .count());
        summary.setNewUsersToday(userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt().isAfter(startOfDay))
                .count());

        summary.setActiveUsers((long) todayTxns.stream()
                .map(Transaction::getAccountId)
                .distinct()
                .count());

        BigDecimal systemBalance = accountRepository.findAll().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setSystemBalance(systemBalance);

        return summary;
    }

    @Override
    public TransactionStatsDto getTransactionStats(LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionDate().isAfter(startDate) &&
                        t.getTransactionDate().isBefore(endDate))
                .collect(Collectors.toList());

        TransactionStatsDto stats = new TransactionStatsDto();
        stats.setStartDate(startDate);
        stats.setEndDate(endDate);
        stats.setTotalTransactions((long) transactions.size());

        BigDecimal totalAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalAmount(totalAmount);

        List<Transaction> deposits = transactions.stream()
                .filter(t -> "DEPOSIT".equals(t.getTransactionType()))
                .collect(Collectors.toList());
        stats.setDepositsCount((long) deposits.size());
        stats.setDepositsTotal(deposits.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        List<Transaction> withdrawals = transactions.stream()
                .filter(t -> "WITHDRAW".equals(t.getTransactionType()))
                .collect(Collectors.toList());
        stats.setWithdrawalsCount((long) withdrawals.size());
        stats.setWithdrawalsTotal(withdrawals.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        List<Transaction> transfers = transactions.stream()
                .filter(t -> "TRANSFER_OUT".equals(t.getTransactionType()))
                .collect(Collectors.toList());
        stats.setTransfersCount((long) transfers.size());
        stats.setTransfersTotal(transfers.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        if (!transactions.isEmpty()) {
            stats.setAverageTransactionAmount(totalAmount.divide(
                    BigDecimal.valueOf(transactions.size()), 2, RoundingMode.HALF_UP));
            stats.setLargestTransaction(transactions.stream()
                    .map(Transaction::getAmount)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO));
            stats.setSmallestTransaction(transactions.stream()
                    .map(Transaction::getAmount)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO));
        }

        return stats;
    }

    @Override
    public List<AccountDto> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList());
    }

    @Override
    public void freezeAccount(Long accountId, String reason, HttpServletRequest httpRequest) {
        log.info("🔵 Admin freezeAccount (alternate method) - ID: {}", accountId);
        accountService.freezeAccount(accountId, reason, "admin", httpRequest);
    }

    @Override
    public List<TransactionDto> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    public void closeAccount(Long accountId, String reason, HttpServletRequest httpRequest) {
        log.info("🔵 Admin closeAccount (alternate method) - ID: {}", accountId);
        accountService.closeAccount(accountId, reason, "admin", httpRequest);
    }

    @Override
    public void approveTransaction(Long transactionId, Boolean approved, String remarks, HttpServletRequest httpRequest) {
        log.info("🔵 START: Approving transaction ID: {}, approved: {}", transactionId, approved);

        try {
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

            transaction.setStatus(approved ? "APPROVED" : "REJECTED");
            transaction.setRemarks(remarks);
            transactionRepository.save(transaction);

            String action = approved ? "TRANSACTION_APPROVED" : "TRANSACTION_REJECTED";
            auditService.logAction("admin", action,
                    "Transaction " + transaction.getId() + " " + action.toLowerCase(), httpRequest, "INFO");

            log.info("🟢 SUCCESS: Transaction {} {}", transactionId, approved ? "approved" : "rejected");

        } catch (Exception e) {
            log.error("🔴 ERROR in approveTransaction: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<TransactionDto> getPendingTransactions() {
        return transactionRepository.findAll().stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    public AdminDashboardDto getAdminDashboard() {
        return getDashboard();
    }

    @Override
    public Map<String, Long> getAccountTypeDistribution() {
        return accountRepository.findAll().stream()
                .collect(Collectors.groupingBy(Account::getAccountType, Collectors.counting()));
    }

    @Override
    public Map<String, Object> getTransactionVolume(String period) {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();

        BigDecimal totalAmount = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionDate().isAfter(start) && t.getTransactionDate().isBefore(end))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "period", period,
                "totalTransactions", transactionRepository.findAll().size(),
                "totalAmount", totalAmount
        );
    }

    @Override
    public AdminUserDto getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToAdminUserDto(user);
    }

    @Transactional
    @Override
    public void enableUser(Long userId, HttpServletRequest request) {
        log.info("🔵 START: Enabling user ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (!user.getEnabled()) {
                user.setEnabled(true);
                userRepository.save(user);

                auditService.logAction("admin", "USER_ENABLED",
                        "User " + user.getUsername() + " enabled", request, "INFO");

                log.info("🟢 SUCCESS: User {} enabled", user.getUsername());
            } else {
                log.info("⚠️ User {} is already enabled", user.getUsername());
            }

        } catch (Exception e) {
            log.error("🔴 ERROR in enableUser: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    @Override
    public void disableUser(Long userId, HttpServletRequest request) {
        log.info("🔵 START: Disabling user ID: {}", userId);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (user.getEnabled()) {
                user.setEnabled(false);
                userRepository.save(user);

                auditService.logAction("admin", "USER_DISABLED",
                        "User " + user.getUsername() + " disabled", request, "INFO");

                log.info("🟢 SUCCESS: User {} disabled", user.getUsername());
            } else {
                log.info("⚠️ User {} is already disabled", user.getUsername());
            }

        } catch (Exception e) {
            log.error("🔴 ERROR in disableUser: {}", e.getMessage(), e);
            throw e;
        }
    }

    private AdminUserDto mapToAdminUserDto(User user) {
        AdminUserDto dto = UserMapper.mapToAdminUserDto(user);

        List<Account> accounts = accountRepository.findByUser_Id(user.getId());
        dto.setTotalAccounts(accounts.size());

        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalBalance(totalBalance);

        return dto;
    }
}