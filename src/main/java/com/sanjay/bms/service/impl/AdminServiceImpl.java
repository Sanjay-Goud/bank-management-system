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

        // User statistics
        dashboard.setTotalUsers(userRepository.count());

        // Account statistics
        dashboard.setTotalAccounts(accountRepository.count());
        dashboard.setActiveAccounts(accountRepository.countByAccountStatus("Active"));
        dashboard.setFrozenAccounts(accountRepository.countByAccountStatus("Frozen"));

        // Total system balance
        BigDecimal totalBalance = accountRepository.findAll().stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTotalSystemBalance(totalBalance);

        // Today's transactions
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        dashboard.setTodayTransactions(transactionRepository.countTransactionsSince(startOfDay));

        // Today's transaction volume
        BigDecimal todayVolume = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionDate().isAfter(startOfDay))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTodayTransactionVolume(todayVolume);

        // Placeholder for pending approvals and alerts
        dashboard.setPendingApprovals(0L);
        dashboard.setCriticalAlerts(auditLogRepository.findBySeverityOrderByTimestampDesc("CRITICAL").size() > 0 ?
                (long) auditLogRepository.findBySeverityOrderByTimestampDesc("CRITICAL").size() : 0L);

        // Recent high-value transactions (>50000)
        List<TransactionDto> highValueTxns = transactionRepository.findAll().stream()
                .filter(t -> t.getAmount().compareTo(new BigDecimal("50000")) > 0)
                .sorted((t1, t2) -> t2.getTransactionDate().compareTo(t1.getTransactionDate()))
                .limit(10)
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
        dashboard.setRecentHighValueTransactions(highValueTxns);

        // Recent critical audit logs
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEnabled(!user.getEnabled());
        userRepository.save(user);

        String action = user.getEnabled() ? "USER_ENABLED" : "USER_DISABLED";
        auditService.logAction("admin", action,
                "User " + user.getUsername() + " status changed", request, "INFO");

        log.info("User {} status toggled to: {}", user.getUsername(), user.getEnabled());
    }

    @Override
    @Transactional
    public void lockUser(Long userId, String reason, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setAccountLocked(true);
        user.setAccountLockedUntil(LocalDateTime.now().plusDays(30)); // Lock for 30 days
        userRepository.save(user);

        notificationService.createNotification(user, "Account Locked",
                "Your account has been locked. Reason: " + reason, "SECURITY");

        auditService.logAction("admin", "USER_LOCKED",
                "User " + user.getUsername() + " locked. Reason: " + reason, request, "CRITICAL");

        log.info("User {} locked. Reason: {}", user.getUsername(), reason);
    }

    @Override
    @Transactional
    public void unlockUser(Long userId, HttpServletRequest request) {
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

        log.info("User {} unlocked", user.getUsername());
    }

    @Override
    @Transactional
    public void freezeAccount(FreezeAccountRequest request, HttpServletRequest httpRequest) {
        accountService.freezeAccount(request.getAccountId(), request.getReason(),
                "admin", httpRequest);
    }

    @Override
    @Transactional
    public void unfreezeAccount(Long accountId, HttpServletRequest request) {
        accountService.unfreezeAccount(accountId, "admin", request);
    }

    @Override
    @Transactional
    public void closeAccount(CloseAccountRequest request, HttpServletRequest httpRequest) {
        accountService.closeAccount(request.getAccountId(), request.getReason(),
                "admin", httpRequest);
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

        // Deposits
        List<Transaction> deposits = todayTxns.stream()
                .filter(t -> "DEPOSIT".equals(t.getTransactionType()))
                .collect(Collectors.toList());
        summary.setDepositsCount((long) deposits.size());
        summary.setDepositsAmount(deposits.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Withdrawals
        List<Transaction> withdrawals = todayTxns.stream()
                .filter(t -> "WITHDRAW".equals(t.getTransactionType()))
                .collect(Collectors.toList());
        summary.setWithdrawalsCount((long) withdrawals.size());
        summary.setWithdrawalsAmount(withdrawals.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Transfers
        List<Transaction> transfers = todayTxns.stream()
                .filter(t -> t.getTransactionType().contains("TRANSFER"))
                .collect(Collectors.toList());
        summary.setTransfersCount((long) transfers.size() / 2); // Divide by 2 as each transfer has 2 records
        summary.setTransfersAmount(transfers.stream()
                .filter(t -> "TRANSFER_OUT".equals(t.getTransactionType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // New accounts and users today
        summary.setNewAccountsToday(accountRepository.findAll().stream()
                .filter(a -> a.getCreatedAt().isAfter(startOfDay))
                .count());
        summary.setNewUsersToday(userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt().isAfter(startOfDay))
                .count());

        // Active users (users who made transactions today)
        summary.setActiveUsers((long) todayTxns.stream()
                .map(Transaction::getAccountId)
                .distinct()
                .count());

        // System balance
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

        // Deposits
        List<Transaction> deposits = transactions.stream()
                .filter(t -> "DEPOSIT".equals(t.getTransactionType()))
                .collect(Collectors.toList());
        stats.setDepositsCount((long) deposits.size());
        stats.setDepositsTotal(deposits.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Withdrawals
        List<Transaction> withdrawals = transactions.stream()
                .filter(t -> "WITHDRAW".equals(t.getTransactionType()))
                .collect(Collectors.toList());
        stats.setWithdrawalsCount((long) withdrawals.size());
        stats.setWithdrawalsTotal(withdrawals.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Transfers
        List<Transaction> transfers = transactions.stream()
                .filter(t -> "TRANSFER_OUT".equals(t.getTransactionType()))
                .collect(Collectors.toList());
        stats.setTransfersCount((long) transfers.size());
        stats.setTransfersTotal(transfers.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // Average, largest, smallest
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
        accountService.closeAccount(accountId, reason, "admin", httpRequest);

    }

    @Override
    public void approveTransaction(Long transactionId, Boolean approved, String remarks, HttpServletRequest httpRequest) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        transaction.setStatus(approved ? "APPROVED" : "REJECTED");
        transaction.setRemarks(remarks);
        transactionRepository.save(transaction);

        // Audit log
        String action = approved ? "TRANSACTION_APPROVED" : "TRANSACTION_REJECTED";
        auditService.logAction("admin", action,
                "Transaction " + transaction.getId() + " " + action.toLowerCase(), httpRequest, "INFO");
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
        LocalDateTime start = LocalDateTime.now().minusDays(1); // default daily
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.getEnabled()) {
            user.setEnabled(true);
            userRepository.save(user);
            auditService.logAction("admin", "USER_ENABLED",
                    "User " + user.getUsername() + " enabled", request, "INFO");
            log.info("User {} enabled", user.getUsername());
        }

    }

    @Transactional
    @Override
    public void disableUser(Long userId, HttpServletRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getEnabled()) {
            user.setEnabled(false);
            userRepository.save(user);
            auditService.logAction("admin", "USER_DISABLED",
                    "User " + user.getUsername() + " disabled", request, "INFO");
            log.info("User {} disabled", user.getUsername());
        }

    }


    private AdminUserDto mapToAdminUserDto(User user) {
        AdminUserDto dto = UserMapper.mapToAdminUserDto(user);

        // Add account and balance info
        List<Account> accounts = accountRepository.findByUser_Id(user.getId());
        dto.setTotalAccounts(accounts.size());

        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalBalance(totalBalance);

        return dto;
    }
}