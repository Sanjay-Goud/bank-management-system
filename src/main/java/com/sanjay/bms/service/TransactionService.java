package com.sanjay.bms.service;

import com.sanjay.bms.dto.*;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {
    // Existing methods
    List<TransactionDto> getTransactionsByAccountId(Long accountId);
    List<TransactionDto> getRecentTransactions(int limit);
    Long getTodayTransactionsCount();
    List<TransactionDto> getTransactionsByDateRange(Long accountId, LocalDateTime startDate, LocalDateTime endDate);
    TransactionDto transferFunds(TransferRequest transferRequest);
    List<TransactionDto> getAllTransactions();
    void recordTransaction(String deposit, Long accountId, BigDecimal amount, BigDecimal newBalance, String depositToAccount);

    // User-specific methods
    List<TransactionDto> getAccountTransactions(Long accountId, String username);
    List<TransactionDto> getRecentUserTransactions(String username, int limit);

    // Transfer methods
    TransactionDto transferFunds(TransferRequestWithOtp request, String username, HttpServletRequest httpRequest);

    // ✅ FIXED: Return type is String
    String initiateTransfer(TransferRequest request, String username);

    // Filter and search methods
    List<TransactionDto> filterTransactions(TransactionFilterDto filter, String username);
    List<TransactionDto> searchTransactions(String searchTerm, String username);
    TransactionDto getTransactionByReference(String reference, String username);

    // Statistics
    TransactionStatsDto getTransactionStats(String username, LocalDateTime startDate, LocalDateTime endDate);
}
