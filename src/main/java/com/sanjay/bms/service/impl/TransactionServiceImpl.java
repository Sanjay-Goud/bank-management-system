package com.sanjay.bms.service.impl;

import com.sanjay.bms.dto.TransactionDto;
import com.sanjay.bms.dto.TransferRequest;
import com.sanjay.bms.entity.Account;
import com.sanjay.bms.entity.Transaction;
import com.sanjay.bms.exception.ResourceNotFoundException;
import com.sanjay.bms.mapper.TransactionMapper;
import com.sanjay.bms.repository.AccountRepository;
import com.sanjay.bms.repository.TransactionRepository;
import com.sanjay.bms.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Override
    public List<TransactionDto> getTransactionsByAccountId(Long accountId) {
        List<Transaction> transactions = transactionRepository.findByAccountIdOrderByTransactionDateDesc(accountId);
        return transactions.stream()
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionDto> getRecentTransactions(int limit) {
        List<Transaction> transactions = transactionRepository.findTop50ByOrderByTransactionDateDesc();
        return transactions.stream()
                .limit(limit)
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    public Long getTodayTransactionsCount() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return transactionRepository.countTransactionsSince(startOfDay);
    }

    @Override
    public List<TransactionDto> getTransactionsByDateRange(Long accountId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Transaction> transactions = transactionRepository.findByAccountIdAndDateRange(accountId, startDate, endDate);
        return transactions.stream()
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransactionDto transferFunds(TransferRequest transferRequest) {
        log.info("Processing transfer from {} to {} amount: {}",
                transferRequest.getFromAccountNumber(),
                transferRequest.getToAccountNumber(),
                transferRequest.getAmount());

        // Validate amount
        if (transferRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        // Find accounts
        Account fromAccount = accountRepository.findByAccountNumber(transferRequest.getFromAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + transferRequest.getFromAccountNumber()));

        Account toAccount = accountRepository.findByAccountNumber(transferRequest.getToAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + transferRequest.getToAccountNumber()));

        // Validate same account
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Check balance
        if (fromAccount.getBalance().compareTo(transferRequest.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: " + fromAccount.getBalance());
        }

        // Check account status
        if (!"Active".equals(fromAccount.getAccountStatus())) {
            throw new IllegalArgumentException("Source account is not active");
        }
        if (!"Active".equals(toAccount.getAccountStatus())) {
            throw new IllegalArgumentException("Destination account is not active");
        }

        String referenceNumber = "TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Debit from source
        BigDecimal newFromBalance = fromAccount.getBalance().subtract(transferRequest.getAmount());
        fromAccount.setBalance(newFromBalance);
        accountRepository.save(fromAccount);

        // Create debit transaction
        Transaction debitTransaction = new Transaction();
        debitTransaction.setTransactionType("TRANSFER_OUT");
        debitTransaction.setAccountId(fromAccount.getId());
        debitTransaction.setAmount(transferRequest.getAmount());
        debitTransaction.setBalanceAfter(newFromBalance);
        debitTransaction.setDescription("Transfer to " + toAccount.getAccountNumber() +
                (transferRequest.getDescription() != null ? " - " + transferRequest.getDescription() : ""));
        debitTransaction.setTransactionDate(LocalDateTime.now());
        debitTransaction.setReferenceNumber(referenceNumber);
        debitTransaction.setToAccountId(toAccount.getId());
        debitTransaction.setStatus("SUCCESS");
        transactionRepository.save(debitTransaction);

        // Credit to destination
        BigDecimal newToBalance = toAccount.getBalance().add(transferRequest.getAmount());
        toAccount.setBalance(newToBalance);
        accountRepository.save(toAccount);

        // Create credit transaction
        Transaction creditTransaction = new Transaction();
        creditTransaction.setTransactionType("TRANSFER_IN");
        creditTransaction.setAccountId(toAccount.getId());
        creditTransaction.setAmount(transferRequest.getAmount());
        creditTransaction.setBalanceAfter(newToBalance);
        creditTransaction.setDescription("Transfer from " + fromAccount.getAccountNumber() +
                (transferRequest.getDescription() != null ? " - " + transferRequest.getDescription() : ""));
        creditTransaction.setTransactionDate(LocalDateTime.now());
        creditTransaction.setReferenceNumber(referenceNumber);
        creditTransaction.setToAccountId(fromAccount.getId());
        creditTransaction.setStatus("SUCCESS");
        transactionRepository.save(creditTransaction);

        log.info("Transfer completed successfully. Reference: {}", referenceNumber);
        return TransactionMapper.mapToTransactionDto(debitTransaction);
    }

    @Override
    public List<TransactionDto> getAllTransactions() {
        List<Transaction> transactions = transactionRepository.findAll();
        return transactions.stream()
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    // Helper method to record transaction
    public void recordTransaction(String type, Long accountId, BigDecimal amount,
                                  BigDecimal balanceAfter, String description) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(type);
        transaction.setAccountId(accountId);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setReferenceNumber("TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setStatus("SUCCESS");
        transactionRepository.save(transaction);
    }
}