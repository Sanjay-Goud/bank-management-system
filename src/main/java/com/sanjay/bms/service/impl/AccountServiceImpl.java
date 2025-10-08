package com.sanjay.bms.service.impl;

import com.sanjay.bms.dto.AccountDto;
import com.sanjay.bms.entity.Account;
import com.sanjay.bms.entity.User;
import com.sanjay.bms.exception.ResourceNotFoundException;
import com.sanjay.bms.repository.AccountRepository;
import com.sanjay.bms.repository.UserRepository;
import com.sanjay.bms.service.NotificationService;
import com.sanjay.bms.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class AccountServiceImpl {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final TransactionServiceImpl transactionService;

    @Transactional
    public AccountDto createAccount(AccountDto accountDto, String username, HttpServletRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setAccountHolderName(accountDto.getAccountHolderName());
        account.setAccountType(accountDto.getAccountType());
        account.setBalance(accountDto.getBalance() != null ? accountDto.getBalance() : BigDecimal.ZERO);
        account.setAccountStatus("Active");
        account.setUser(user);
        account.setCreatedAt(LocalDateTime.now());
        account.setDailyTransactionLimit(new BigDecimal("100000"));
        account.setPerTransactionLimit(new BigDecimal("50000"));
        account.setDailyTransactionTotal(BigDecimal.ZERO);
        account.setDailyLimitResetDate(LocalDateTime.now());

        // Set interest rate based on account type
        if ("SAVINGS".equals(accountDto.getAccountType())) {
            account.setInterestRate(new BigDecimal("4.5"));
            account.setMinimumBalance(new BigDecimal("1000"));
        } else if ("CHECKING".equals(accountDto.getAccountType())) {
            account.setInterestRate(new BigDecimal("0"));
            account.setMinimumBalance(BigDecimal.ZERO);
        }

        Account savedAccount = accountRepository.save(account);

        // Create notification
        notificationService.createNotification(user, "New Account Created",
                "Your new " + account.getAccountType() + " account has been created successfully. Account Number: " +
                        maskAccountNumber(savedAccount.getAccountNumber()), "ACCOUNT");

        // Audit log
        auditService.logAccountAction(username, "ACCOUNT_CREATED", savedAccount.getId(),
                "Account created: " + savedAccount.getAccountNumber(), request);

        return mapToDto(savedAccount);
    }

    public List<AccountDto> getUserAccounts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return accountRepository.findByUserId(user.getId()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public AccountDto getAccountById(Long id, String username) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Verify ownership
        if (!account.getUser().getUsername().equals(username)) {
            throw new SecurityException("Access denied");
        }

        return mapToDto(account);
    }

    @Transactional
    public AccountDto deposit(Long accountId, BigDecimal amount, String username,
                              HttpServletRequest request) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Verify ownership
        if (!account.getUser().getUsername().equals(username)) {
            throw new SecurityException("Access denied");
        }

        // Check account status
        if (!"Active".equals(account.getAccountStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        // Check transaction limit
        validateTransactionLimit(account, amount);

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        account.setLastTransactionDate(LocalDateTime.now());
        updateDailyTotal(account, amount);

        Account savedAccount = accountRepository.save(account);

        // Record transaction
        transactionService.recordTransaction("DEPOSIT", accountId, amount, newBalance,
                "Deposit to account");

        // Send notification
        notificationService.notifyDeposit(account.getUser(), account.getAccountNumber(), amount);

        // Audit log
        auditService.logTransaction(username, "DEPOSIT", accountId, amount.toString(), request);

        // Check for high-value transaction
        if (amount.compareTo(new BigDecimal("50000")) > 0) {
            notificationService.notifyHighValueTransaction(account.getUser(), amount, "DEPOSIT");
        }

        return mapToDto(savedAccount);
    }

    @Transactional
    public AccountDto withdraw(Long accountId, BigDecimal amount, String username,
                               HttpServletRequest request) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Verify ownership
        if (!account.getUser().getUsername().equals(username)) {
            throw new SecurityException("Access denied");
        }

        // Check account status
        if (!"Active".equals(account.getAccountStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        // Check transaction limit
        validateTransactionLimit(account, amount);

        // Check balance
        BigDecimal balanceAfterWithdrawal = account.getBalance().subtract(amount);
        if (balanceAfterWithdrawal.compareTo(account.getMinimumBalance()) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Minimum balance required: " +
                    account.getMinimumBalance());
        }

        account.setBalance(balanceAfterWithdrawal);
        account.setLastTransactionDate(LocalDateTime.now());
        updateDailyTotal(account, amount);

        Account savedAccount = accountRepository.save(account);

        // Record transaction
        transactionService.recordTransaction("WITHDRAW", accountId, amount, balanceAfterWithdrawal,
                "Withdrawal from account");

        // Send notification
        notificationService.notifyWithdrawal(account.getUser(), account.getAccountNumber(), amount);

        // Audit log
        auditService.logTransaction(username, "WITHDRAW", accountId, amount.toString(), request);

        // Check for high-value transaction
        if (amount.compareTo(new BigDecimal("50000")) > 0) {
            notificationService.notifyHighValueTransaction(account.getUser(), amount, "WITHDRAWAL");
        }

        return mapToDto(savedAccount);
    }

    @Transactional
    public void freezeAccount(Long accountId, String reason, String adminUsername,
                              HttpServletRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setAccountStatus("Frozen");
        account.setFrozenReason(reason);
        account.setFrozenAt(LocalDateTime.now());
        accountRepository.save(account);

        // Notify user
        notificationService.notifyAccountFrozen(account.getUser(), account.getAccountNumber(), reason);

        // Audit log
        auditService.logAccountAction(adminUsername, "ACCOUNT_FROZEN", accountId,
                "Account frozen. Reason: " + reason, request);
    }

    @Transactional
    public void unfreezeAccount(Long accountId, String adminUsername, HttpServletRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setAccountStatus("Active");
        account.setFrozenReason(null);
        account.setFrozenAt(null);
        accountRepository.save(account);

        // Notify user
        notificationService.createNotification(account.getUser(), "Account Unfrozen",
                "Your account " + maskAccountNumber(account.getAccountNumber()) + " has been unfrozen",
                "ACCOUNT");

        // Audit log
        auditService.logAccountAction(adminUsername, "ACCOUNT_UNFROZEN", accountId,
                "Account unfrozen", request);
    }

    @Transactional
    public void closeAccount(Long accountId, String reason, String adminUsername,
                             HttpServletRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Check if balance is zero
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("Cannot close account with non-zero balance");
        }

        account.setAccountStatus("Closed");
        account.setClosureReason(reason);
        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);

        // Notify user
        notificationService.createNotification(account.getUser(), "Account Closed",
                "Your account " + maskAccountNumber(account.getAccountNumber()) + " has been closed. Reason: " + reason,
                "ACCOUNT");

        // Audit log
        auditService.logAccountAction(adminUsername, "ACCOUNT_CLOSED", accountId,
                "Account closed. Reason: " + reason, request);
    }

    private void validateTransactionLimit(Account account, BigDecimal amount) {
        // Reset daily total if new day
        if (account.getDailyLimitResetDate().toLocalDate().isBefore(LocalDate.now())) {
            account.setDailyTransactionTotal(BigDecimal.ZERO);
            account.setDailyLimitResetDate(LocalDateTime.now());
        }

        // Check per-transaction limit
        if (amount.compareTo(account.getPerTransactionLimit()) > 0) {
            throw new IllegalArgumentException("Transaction amount exceeds per-transaction limit of " +
                    account.getPerTransactionLimit());
        }

        // Check daily limit
        BigDecimal newDailyTotal = account.getDailyTransactionTotal().add(amount);
        if (newDailyTotal.compareTo(account.getDailyTransactionLimit()) > 0) {
            throw new IllegalArgumentException("Daily transaction limit exceeded. Limit: " +
                    account.getDailyTransactionLimit() + ", Used: " + account.getDailyTransactionTotal());
        }
    }

    private void updateDailyTotal(Account account, BigDecimal amount) {
        account.setDailyTransactionTotal(account.getDailyTransactionTotal().add(amount));
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder accountNumber = new StringBuilder("ACC");
        for (int i = 0; i < 9; i++) {
            accountNumber.append(random.nextInt(10));
        }

        // Ensure uniqueness
        while (accountRepository.findByAccountNumber(accountNumber.toString()).isPresent()) {
            accountNumber = new StringBuilder("ACC");
            for (int i = 0; i < 9; i++) {
                accountNumber.append(random.nextInt(10));
            }
        }

        return accountNumber.toString();
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        String first4 = accountNumber.substring(0, 4);
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return first4 + "****" + last4;
    }

    private AccountDto mapToDto(Account account) {
        AccountDto dto = new AccountDto();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setMaskedAccountNumber(maskAccountNumber(account.getAccountNumber()));
        dto.setAccountHolderName(account.getAccountHolderName());
        dto.setAccountType(account.getAccountType());
        dto.setBalance(account.getBalance());
        dto.setAccountStatus(account.getAccountStatus());
        dto.setCreatedAt(account.getCreatedAt());
        dto.setLastTransactionDate(account.getLastTransactionDate());
        dto.setDailyTransactionLimit(account.getDailyTransactionLimit());
        dto.setPerTransactionLimit(account.getPerTransactionLimit());
        dto.setInterestRate(account.getInterestRate());
        dto.setMinimumBalance(account.getMinimumBalance());
        dto.setUserId(account.getUser().getUsername());
        return dto;
    }
}