package com.sanjay.bms.service.impl;

import com.sanjay.bms.dto.AccountDto;
import com.sanjay.bms.entity.Account;
import com.sanjay.bms.entity.User;
import com.sanjay.bms.exception.InsufficientBalanceException;
import com.sanjay.bms.exception.ResourceNotFoundException;
import com.sanjay.bms.mapper.AccountMapper;
import com.sanjay.bms.repository.AccountRepository;
import com.sanjay.bms.repository.UserRepository;
import com.sanjay.bms.service.AccountService;
import com.sanjay.bms.service.AuditService;
import com.sanjay.bms.service.NotificationService;
import com.sanjay.bms.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ✅ UPDATED: createAccount with username and request
    @Override
    @Transactional
    public AccountDto createAccount(AccountDto accountDto, String username, HttpServletRequest request) {
        log.info("Creating new account for user: {}", username);

        // Find user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Account account = new Account();
        account.setAccountNumber(generateAccountNumber());
        account.setAccountHolderName(accountDto.getAccountHolderName() != null ?
                accountDto.getAccountHolderName() : user.getFullName());
        account.setAccountType(accountDto.getAccountType());
        account.setBalance(accountDto.getBalance() != null ? accountDto.getBalance() : BigDecimal.ZERO);
        account.setAccountStatus("Active");
        account.setUser(user);
        account.setCreatedAt(LocalDateTime.now());
        account.setDailyTransactionLimit(accountDto.getDailyTransactionLimit() != null ?
                accountDto.getDailyTransactionLimit() : new BigDecimal("100000"));
        account.setPerTransactionLimit(accountDto.getPerTransactionLimit() != null ?
                accountDto.getPerTransactionLimit() : new BigDecimal("50000"));
        account.setInterestRate(accountDto.getInterestRate());
        account.setMinimumBalance(accountDto.getMinimumBalance() != null ?
                accountDto.getMinimumBalance() : BigDecimal.ZERO);

        Account savedAccount = accountRepository.save(account);

        // Audit log
        auditService.logAction(username, "ACCOUNT_CREATED",
                "Account created: " + savedAccount.getAccountNumber(), request, "INFO");

        // Notification
        notificationService.createNotification(user, "Account Created",
                "Your " + accountDto.getAccountType() + " account has been created successfully. " +
                        "Account number: " + savedAccount.getAccountNumber(), "ACCOUNT");

        log.info("Account created successfully: {}", savedAccount.getAccountNumber());
        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public List<AccountDto> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList());
    }

    // ✅ NEW: Get user's accounts
    @Override
    public List<AccountDto> getUserAccounts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return accountRepository.findByUser_Id(user.getId()).stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList());
    }

    // ✅ NEW: Get account by ID with ownership verification
    @Override
    public AccountDto getAccountById(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        // Verify ownership (unless admin)
        if (!account.getUserId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new SecurityException("Access denied to this account");
        }

        return AccountMapper.mapToAccountDto(account);
    }

    // Keep backward compatibility
    @Override
    public AccountDto getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    @Transactional
    public AccountDto updateAccount(Long id, AccountDto accountDto) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        // Update allowed fields
        if (accountDto.getAccountHolderName() != null) {
            account.setAccountHolderName(accountDto.getAccountHolderName());
        }
        if (accountDto.getAccountType() != null) {
            account.setAccountType(accountDto.getAccountType());
        }
        if (accountDto.getAccountStatus() != null) {
            account.setAccountStatus(accountDto.getAccountStatus());
        }
        if (accountDto.getDailyTransactionLimit() != null) {
            account.setDailyTransactionLimit(accountDto.getDailyTransactionLimit());
        }
        if (accountDto.getPerTransactionLimit() != null) {
            account.setPerTransactionLimit(accountDto.getPerTransactionLimit());
        }
        if (accountDto.getInterestRate() != null) {
            account.setInterestRate(accountDto.getInterestRate());
        }
        if (accountDto.getMinimumBalance() != null) {
            account.setMinimumBalance(accountDto.getMinimumBalance());
        }

        Account updatedAccount = accountRepository.save(account);
        log.info("Account updated: {}", updatedAccount.getAccountNumber());

        return AccountMapper.mapToAccountDto(updatedAccount);
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        // Check if account has balance
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Cannot delete account with non-zero balance");
        }

        accountRepository.deleteById(id);
        log.info("Account deleted: {}", account.getAccountNumber());
    }

    @Override
    public AccountDto findByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));
        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    public List<AccountDto> findByAccountTypeIgnoreCase(String accountType) {
        return accountRepository.findByAccountTypeIgnoreCase(accountType).stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList());
    }

    // ✅ UPDATED: deposit with username and request
    @Override
    @Transactional
    public AccountDto deposit(Long id, BigDecimal amount, String username, HttpServletRequest request) {
        log.info("Processing deposit for account ID: {}, amount: {}, user: {}", id, amount, username);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        // Verify ownership (unless admin)
        if (!account.getUserId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new SecurityException("Access denied to this account");
        }

        if (!"Active".equals(account.getAccountStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);
        account.setLastTransactionDate(LocalDateTime.now());

        Account updatedAccount = accountRepository.save(account);

        // Record transaction
        transactionService.recordTransaction("DEPOSIT", id, amount, newBalance,
                "Deposit to account " + account.getAccountNumber());

        // Audit log
        auditService.logTransaction(username, "DEPOSIT", id, amount.toString(), request);

        // Send notification
        notificationService.notifyDeposit(account.getUser(), account.getAccountNumber(), amount);

        log.info("Deposit successful. New balance: {}", newBalance);

        return AccountMapper.mapToAccountDto(updatedAccount);
    }

    // ✅ UPDATED: withdraw with username and request
    @Override
    @Transactional
    public AccountDto withdraw(Long id, BigDecimal amount, String username, HttpServletRequest request) {
        log.info("Processing withdrawal for account ID: {}, amount: {}, user: {}", id, amount, username);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }

        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        // Verify ownership (unless admin)
        if (!account.getUserId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new SecurityException("Access denied to this account");
        }

        if (!"Active".equals(account.getAccountStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        // Check balance
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance. Available: " + account.getBalance());
        }

        // Check minimum balance
        BigDecimal balanceAfterWithdrawal = account.getBalance().subtract(amount);
        if (balanceAfterWithdrawal.compareTo(account.getMinimumBalance()) < 0) {
            throw new IllegalArgumentException("Cannot withdraw. Minimum balance requirement: " +
                    account.getMinimumBalance());
        }

        // Check transaction limit
        if (amount.compareTo(account.getPerTransactionLimit()) > 0) {
            throw new IllegalArgumentException("Amount exceeds per-transaction limit: " +
                    account.getPerTransactionLimit());
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        account.setLastTransactionDate(LocalDateTime.now());

        Account updatedAccount = accountRepository.save(account);

        // Record transaction
        transactionService.recordTransaction("WITHDRAW", id, amount, newBalance,
                "Withdrawal from account " + account.getAccountNumber());

        // Audit log
        auditService.logTransaction(username, "WITHDRAW", id, amount.toString(), request);

        // Send notification
        notificationService.notifyWithdrawal(account.getUser(), account.getAccountNumber(), amount);

        log.info("Withdrawal successful. New balance: {}", newBalance);

        return AccountMapper.mapToAccountDto(updatedAccount);
    }

    @Override
    public Long getTotalAccountCount() {
        return accountRepository.count();
    }

    @Override
    public List<AccountDto> getAccountsAboveBalance(BigDecimal minBalance) {
        return accountRepository.findByBalanceGreaterThanEqual(minBalance).stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void freezeAccount(Long accountId, String reason, String performedBy, HttpServletRequest request) {
        log.info("Freezing account ID: {} by {}", accountId, performedBy);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        if ("Frozen".equals(account.getAccountStatus())) {
            throw new IllegalArgumentException("Account is already frozen");
        }

        account.setAccountStatus("Frozen");
        account.setFrozenReason(reason);
        account.setFrozenAt(LocalDateTime.now());
        accountRepository.save(account);

        // Audit log
        auditService.logAccountAction(performedBy, "ACCOUNT_FROZEN", accountId,
                "Account " + account.getAccountNumber() + " frozen. Reason: " + reason, request);

        // Notify user
        notificationService.notifyAccountFrozen(account.getUser(), account.getAccountNumber(), reason);

        log.info("Account frozen successfully: {}", account.getAccountNumber());
    }

    @Override
    @Transactional
    public void unfreezeAccount(Long accountId, String performedBy, HttpServletRequest request) {
        log.info("Unfreezing account ID: {} by {}", accountId, performedBy);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        if (!"Frozen".equals(account.getAccountStatus())) {
            throw new IllegalArgumentException("Account is not frozen");
        }

        account.setAccountStatus("Active");
        account.setFrozenReason(null);
        account.setFrozenAt(null);
        accountRepository.save(account);

        // Audit log
        auditService.logAccountAction(performedBy, "ACCOUNT_UNFROZEN", accountId,
                "Account " + account.getAccountNumber() + " unfrozen", request);

        // Notify user
        notificationService.createNotification(account.getUser(), "Account Unfrozen",
                "Your account " + account.getAccountNumber() + " has been unfrozen and is now active.",
                "ACCOUNT");

        log.info("Account unfrozen successfully: {}", account.getAccountNumber());
    }

    @Override
    @Transactional
    public void closeAccount(Long accountId, String reason, String performedBy, HttpServletRequest request) {
        log.info("Closing account ID: {} by {}", accountId, performedBy);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        if ("Closed".equals(account.getAccountStatus())) {
            throw new IllegalArgumentException("Account is already closed");
        }

        // Check balance
        if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Cannot close account with non-zero balance. " +
                    "Please withdraw all funds first.");
        }

        account.setAccountStatus("Closed");
        account.setClosureReason(reason);
        account.setClosedAt(LocalDateTime.now());
        accountRepository.save(account);

        // Audit log
        auditService.logAccountAction(performedBy, "ACCOUNT_CLOSED", accountId,
                "Account " + account.getAccountNumber() + " closed. Reason: " + reason, request);

        // Notify user
        notificationService.createNotification(account.getUser(), "Account Closed",
                "Your account " + account.getAccountNumber() + " has been closed. Reason: " + reason,
                "ACCOUNT");

        log.info("Account closed successfully: {}", account.getAccountNumber());
    }

    private String generateAccountNumber() {
        // Generate 12-digit account number
        Random random = new Random();
        StringBuilder accountNumber = new StringBuilder("AC");
        for (int i = 0; i < 10; i++) {
            accountNumber.append(random.nextInt(10));
        }

        // Ensure uniqueness
        while (accountRepository.findByAccountNumber(accountNumber.toString()).isPresent()) {
            accountNumber = new StringBuilder("AC");
            for (int i = 0; i < 10; i++) {
                accountNumber.append(random.nextInt(10));
            }
        }

        return accountNumber.toString();
    }
}