package com.sanjay.bms.service;

import com.sanjay.bms.dto.TransactionDto;
import com.sanjay.bms.dto.TransferRequest;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionService {
    List<TransactionDto> getTransactionsByAccountId(Long accountId);
    List<TransactionDto> getRecentTransactions(int limit);
    Long getTodayTransactionsCount();
    List<TransactionDto> getTransactionsByDateRange(Long accountId, LocalDateTime startDate, LocalDateTime endDate);
    TransactionDto transferFunds(TransferRequest transferRequest);
    List<TransactionDto> getAllTransactions();
}