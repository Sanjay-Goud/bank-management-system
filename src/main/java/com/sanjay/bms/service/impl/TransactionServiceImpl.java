package com.sanjay.bms.service.impl;

import com.sanjay.bms.dto.*;
import com.sanjay.bms.entity.Account;
import com.sanjay.bms.entity.Transaction;
import com.sanjay.bms.entity.User;
import com.sanjay.bms.exception.ResourceNotFoundException;
import com.sanjay.bms.mapper.TransactionMapper;
import com.sanjay.bms.repository.AccountRepository;
import com.sanjay.bms.repository.TransactionRepository;
import com.sanjay.bms.repository.UserRepository;
import com.sanjay.bms.service.TransactionService;
import com.sanjay.bms.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final UserRepository userRepository;
    private final OtpService otpService;

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
        if (transferRequest == null) {
            throw new IllegalArgumentException("Transfer request cannot be null");
        }
        if (transferRequest.getFromAccountNumber() == null) {
            throw new IllegalArgumentException("From account number cannot be null");
        }
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

    @Override
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

    @Override
    public List<TransactionDto> getAccountTransactions(Long accountId, String username) {
        // Verify user owns this account
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied to this account");
        }

        return getTransactionsByAccountId(accountId);
    }

    @Override
    public List<TransactionDto> getRecentUserTransactions(String username, int limit) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Account> userAccounts = accountRepository.findByUser_Id(user.getId());
        List<Long> accountIds = userAccounts.stream()
                .map(Account::getId)
                .collect(Collectors.toList());

        List<Transaction> transactions = transactionRepository.findByAccountIdInOrderByTransactionDateDesc(accountIds);

        return transactions.stream()
                .limit(limit)
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TransactionDto transferFunds(TransferRequestWithOtp request, String username, HttpServletRequest httpRequest) {
        log.info("Transfer with OTP from user: {}", username);

        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify OTP
        if (request.getOtpCode() == null || request.getTransactionRef() == null) {
            throw new IllegalArgumentException("OTP code and transaction reference are required");
        }

        if (!otpService.verifyTransactionOtp(user, request.getOtpCode(), request.getTransactionRef())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        // Find the pending transaction
        Transaction pendingTransaction = transactionRepository.findByReferenceNumber(request.getTransactionRef())
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        if (!"PENDING".equals(pendingTransaction.getStatus())) {
            throw new IllegalArgumentException("Transaction is not in PENDING state");
        }

        // Get accounts
        Account fromAccount = accountRepository.findById(pendingTransaction.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        Account toAccount = accountRepository.findById(pendingTransaction.getToAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        // Verify ownership again
        if (!fromAccount.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("You don't own the source account");
        }

        // Check balance again (in case it changed)
        if (fromAccount.getBalance().compareTo(pendingTransaction.getAmount()) < 0) {
            pendingTransaction.setStatus("FAILED");
            transactionRepository.save(pendingTransaction);
            throw new IllegalArgumentException("Insufficient balance");
        }

        // Complete the transfer
        TransferRequest basicRequest = new TransferRequest();
        basicRequest.setFromAccountNumber(fromAccount.getAccountNumber());
        basicRequest.setToAccountNumber(toAccount.getAccountNumber());
        basicRequest.setAmount(pendingTransaction.getAmount());
        basicRequest.setDescription(pendingTransaction.getDescription());

        completeTransfer(pendingTransaction, fromAccount, toAccount, basicRequest);

        log.info("Transfer completed with OTP. Reference: {}", request.getTransactionRef());
        return TransactionMapper.mapToTransactionDto(pendingTransaction);
    }

    @Override
    @Transactional
    public String initiateTransfer(TransferRequest request, String username) {
        log.info("Initiating transfer for user: {}", username);

        // Validate user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Validate accounts
        Account fromAccount = accountRepository.findByAccountNumber(request.getFromAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        Account toAccount = accountRepository.findByAccountNumber(request.getToAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));

        // Verify ownership
        if (!fromAccount.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("You don't own the source account");
        }

        // Validate transfer
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: " + fromAccount.getBalance());
        }

        if (!"Active".equals(fromAccount.getAccountStatus())) {
            throw new IllegalArgumentException("Source account is not active");
        }

        if (!"Active".equals(toAccount.getAccountStatus())) {
            throw new IllegalArgumentException("Destination account is not active");
        }

        // Generate transaction reference
        String transactionRef = "TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Create pending transaction
        Transaction pendingTransaction = new Transaction();
        pendingTransaction.setTransactionType("TRANSFER_OUT");
        pendingTransaction.setAccountId(fromAccount.getId());
        pendingTransaction.setAmount(request.getAmount());
        pendingTransaction.setBalanceAfter(fromAccount.getBalance().subtract(request.getAmount()));
        pendingTransaction.setDescription("Pending transfer to " + toAccount.getAccountNumber() +
                (request.getDescription() != null ? " - " + request.getDescription() : ""));
        pendingTransaction.setTransactionDate(LocalDateTime.now());
        pendingTransaction.setReferenceNumber(transactionRef);
        pendingTransaction.setToAccountId(toAccount.getId());
        pendingTransaction.setStatus("PENDING");
        transactionRepository.save(pendingTransaction);

        // Check if OTP is required (for high-value transactions)
        BigDecimal highValueThreshold = new BigDecimal("25000");
        if (request.getAmount().compareTo(highValueThreshold) > 0) {
            // Generate and send OTP
            otpService.generateTransactionOtp(user, transactionRef);

            log.info("High-value transfer initiated. OTP sent. Ref: {}", transactionRef);
            return transactionRef;
        }

        // For low-value transfers, complete immediately
        completeTransfer(pendingTransaction, fromAccount, toAccount, request);

        log.info("Transfer completed immediately. Reference: {}", transactionRef);
        return transactionRef;
    }

    private void completeTransfer(Transaction pendingTransaction, Account fromAccount,
                                  Account toAccount, TransferRequest request) {
        // Update pending transaction status
        pendingTransaction.setStatus("SUCCESS");

        // Debit from source
        BigDecimal newFromBalance = fromAccount.getBalance().subtract(request.getAmount());
        fromAccount.setBalance(newFromBalance);
        accountRepository.save(fromAccount);

        // Update debit transaction with actual balance
        pendingTransaction.setBalanceAfter(newFromBalance);
        transactionRepository.save(pendingTransaction);

        // Credit to destination
        BigDecimal newToBalance = toAccount.getBalance().add(request.getAmount());
        toAccount.setBalance(newToBalance);
        accountRepository.save(toAccount);

        // Create credit transaction
        Transaction creditTransaction = new Transaction();
        creditTransaction.setTransactionType("TRANSFER_IN");
        creditTransaction.setAccountId(toAccount.getId());
        creditTransaction.setAmount(request.getAmount());
        creditTransaction.setBalanceAfter(newToBalance);
        creditTransaction.setDescription("Transfer from " + fromAccount.getAccountNumber() +
                (request.getDescription() != null ? " - " + request.getDescription() : ""));
        creditTransaction.setTransactionDate(LocalDateTime.now());
        creditTransaction.setReferenceNumber(pendingTransaction.getReferenceNumber());
        creditTransaction.setToAccountId(fromAccount.getId());
        creditTransaction.setStatus("SUCCESS");
        transactionRepository.save(creditTransaction);

        log.info("Transfer completed. Reference: {}", pendingTransaction.getReferenceNumber());
    }

    @Override
    public List<TransactionDto> filterTransactions(TransactionFilterDto filter, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Account> userAccounts = accountRepository.findByUser_Id(user.getId());
        List<Long> accountIds = userAccounts.stream()
                .map(Account::getId)
                .collect(Collectors.toList());

        List<Transaction> transactions = transactionRepository.findByAccountIdInOrderByTransactionDateDesc(accountIds);

        return transactions.stream()
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionDto> searchTransactions(String searchTerm, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Account> userAccounts = accountRepository.findByUser_Id(user.getId());
        List<Long> accountIds = userAccounts.stream()
                .map(Account::getId)
                .collect(Collectors.toList());

        List<Transaction> transactions = transactionRepository.findByAccountIdInOrderByTransactionDateDesc(accountIds);

        return transactions.stream()
                .filter(t -> t.getDescription().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        t.getReferenceNumber().toLowerCase().contains(searchTerm.toLowerCase()))
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionDto getTransactionByReference(String reference, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Transaction transaction = transactionRepository.findByReferenceNumber(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        Account account = accountRepository.findById(transaction.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getUserId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied to this transaction");
        }

        return TransactionMapper.mapToTransactionDto(transaction);
    }

    @Override
    public TransactionStatsDto getTransactionStats(String username, LocalDateTime startDate, LocalDateTime endDate) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Account> userAccounts = accountRepository.findByUser_Id(user.getId());
        List<Long> accountIds = userAccounts.stream()
                .map(Account::getId)
                .collect(Collectors.toList());

        List<Transaction> transactions = transactionRepository.findByAccountIdInAndDateRange(accountIds, startDate, endDate);

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        long totalCount = transactions.size();

        for (Transaction t : transactions) {
            if ("TRANSFER_OUT".equals(t.getTransactionType()) || "WITHDRAWAL".equals(t.getTransactionType())) {
                totalDebit = totalDebit.add(t.getAmount());
            } else if ("TRANSFER_IN".equals(t.getTransactionType()) || "DEPOSIT".equals(t.getTransactionType())) {
                totalCredit = totalCredit.add(t.getAmount());
            }
        }

        TransactionStatsDto stats = new TransactionStatsDto();
        stats.setTotalDebit(totalDebit);
        stats.setTotalCredit(totalCredit);
        stats.setTotalCount(totalCount);
        stats.setNetAmount(totalCredit.subtract(totalDebit));
        stats.setStartDate(startDate);
        stats.setEndDate(endDate);

        return stats;
    }
}