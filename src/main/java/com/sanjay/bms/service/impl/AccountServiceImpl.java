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
import org.springframework.cache.annotation.CacheEvict;
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

    @Override
    @Transactional
    public AccountDto createAccount(AccountDto accountDto, String username, HttpServletRequest request) {
        log.info("Creating new account for user: {}", username);

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

        auditService.logAction(username, "ACCOUNT_CREATED",
                "Account created: " + savedAccount.getAccountNumber(), request, "INFO");

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

    @Override
    public List<AccountDto> getUserAccounts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return accountRepository.findByUser_Id(user.getId()).stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList());
    }

    @Override
    public AccountDto getAccountById(Long id, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));

        if (!account.getUserId().equals(user.getId()) && !"ADMIN".equals(user.getRole())) {
            throw new SecurityException("Access denied to this account");
        }

        return AccountMapper.mapToAccountDto(account);
    }

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

    // ✅ FIXED: Simplified deposit method - removed complex ownership checks for admins
    @Override
    @Transactional
    @CacheEvict(value = "userDashboard", key = "#username", beforeInvocation = true)
    public AccountDto deposit(Long id, BigDecimal amount, String username, HttpServletRequest request) {
        log.info("🔵 START: Processing deposit for account ID: {}, amount: {}, user: {}", id, amount, username);

        try {
            // Validate amount
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("❌ Invalid amount: {}", amount);
                throw new IllegalArgumentException("Deposit amount must be greater than zero");
            }

            // Find user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            log.info("✅ User found: {}, role: {}", username, user.getRole());

            // Find account
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
            log.info("✅ Account found: {}, current balance: {}", account.getAccountNumber(), account.getBalance());

            // ✅ FIXED: Only check ownership for non-admin users
            if (!"ADMIN".equals(user.getRole())) {
                if (!account.getUserId().equals(user.getId())) {
                    log.error("❌ Access denied - User {} trying to access account owned by user {}",
                            user.getId(), account.getUserId());
                    throw new SecurityException("Access denied to this account");
                }
            }

            // Check account status
            if (!"Active".equals(account.getAccountStatus())) {
                log.error("❌ Account is not active: {}", account.getAccountStatus());
                throw new IllegalArgumentException("Account is not active");
            }

            // Perform deposit
            BigDecimal newBalance = account.getBalance().add(amount);
            account.setBalance(newBalance);
            account.setLastTransactionDate(LocalDateTime.now());

            log.info("💰 Updating balance: {} -> {}", account.getBalance(), newBalance);

            Account updatedAccount = accountRepository.save(account);
            log.info("✅ Account saved with new balance: {}", updatedAccount.getBalance());

            // Record transaction
            transactionService.recordTransaction("DEPOSIT", id, amount, newBalance,
                    "Deposit to account " + account.getAccountNumber());
            log.info("✅ Transaction recorded");

            // Audit log
            auditService.logTransaction(username, "DEPOSIT", id, amount.toString(), request);
            log.info("✅ Audit log created");

            // Send notification
            notificationService.notifyDeposit(account.getUser(), account.getAccountNumber(), amount);
            log.info("✅ Notification sent");

            log.info("🟢 SUCCESS: Deposit completed. New balance: {}", newBalance);

            return AccountMapper.mapToAccountDto(updatedAccount);

        } catch (Exception e) {
            log.error("🔴 ERROR in deposit: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ✅ FIXED: Simplified withdraw method
    @Override
    @Transactional
    @CacheEvict(value = "userDashboard", key = "#username", beforeInvocation = true)
    public AccountDto withdraw(Long id, BigDecimal amount, String username, HttpServletRequest request) {
        log.info("🔵 START: Processing withdrawal for account ID: {}, amount: {}, user: {}", id, amount, username);

        try {
            // Validate amount
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("❌ Invalid amount: {}", amount);
                throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
            }

            // Find user
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
            log.info("✅ User found: {}, role: {}", username, user.getRole());

            // Find account
            Account account = accountRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
            log.info("✅ Account found: {}, current balance: {}", account.getAccountNumber(), account.getBalance());

            // ✅ FIXED: Only check ownership for non-admin users
            if (!"ADMIN".equals(user.getRole())) {
                if (!account.getUserId().equals(user.getId())) {
                    log.error("❌ Access denied");
                    throw new SecurityException("Access denied to this account");
                }
            }

            // Check account status
            if (!"Active".equals(account.getAccountStatus())) {
                log.error("❌ Account is not active");
                throw new IllegalArgumentException("Account is not active");
            }

            // Check balance
            if (account.getBalance().compareTo(amount) < 0) {
                log.error("❌ Insufficient balance");
                throw new InsufficientBalanceException("Insufficient balance. Available: " + account.getBalance());
            }

            // Check minimum balance
            BigDecimal balanceAfterWithdrawal = account.getBalance().subtract(amount);
            if (balanceAfterWithdrawal.compareTo(account.getMinimumBalance()) < 0) {
                log.error("❌ Minimum balance violation");
                throw new IllegalArgumentException("Cannot withdraw. Minimum balance requirement: " +
                        account.getMinimumBalance());
            }

            // Check transaction limit
            if (amount.compareTo(account.getPerTransactionLimit()) > 0) {
                log.error("❌ Transaction limit exceeded");
                throw new IllegalArgumentException("Amount exceeds per-transaction limit: " +
                        account.getPerTransactionLimit());
            }

            BigDecimal newBalance = account.getBalance().subtract(amount);
            account.setBalance(newBalance);
            account.setLastTransactionDate(LocalDateTime.now());

            Account updatedAccount = accountRepository.save(account);
            log.info("✅ Withdrawal successful. New balance: {}", newBalance);

            transactionService.recordTransaction("WITHDRAW", id, amount, newBalance,
                    "Withdrawal from account " + account.getAccountNumber());

            auditService.logTransaction(username, "WITHDRAW", id, amount.toString(), request);

            notificationService.notifyWithdrawal(account.getUser(), account.getAccountNumber(), amount);

            log.info("🟢 SUCCESS: Withdrawal completed. New balance: {}", newBalance);

            return AccountMapper.mapToAccountDto(updatedAccount);

        } catch (Exception e) {
            log.error("🔴 ERROR in withdrawal: {}", e.getMessage(), e);
            throw e;
        }
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
        log.info("🔵 START: Freezing account ID: {} by {}", accountId, performedBy);

        try {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

            if ("Frozen".equals(account.getAccountStatus())) {
                log.warn("⚠️ Account is already frozen");
                throw new IllegalArgumentException("Account is already frozen");
            }

            account.setAccountStatus("Frozen");
            account.setFrozenReason(reason);
            account.setFrozenAt(LocalDateTime.now());
            accountRepository.save(account);

            log.info("✅ Account status updated to Frozen");

            auditService.logAccountAction(performedBy, "ACCOUNT_FROZEN", accountId,
                    "Account " + account.getAccountNumber() + " frozen. Reason: " + reason, request);

            notificationService.notifyAccountFrozen(account.getUser(), account.getAccountNumber(), reason);

            log.info("🟢 SUCCESS: Account frozen successfully: {}", account.getAccountNumber());

        } catch (Exception e) {
            log.error("🔴 ERROR in freezeAccount: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void unfreezeAccount(Long accountId, String performedBy, HttpServletRequest request) {
        log.info("🔵 START: Unfreezing account ID: {} by {}", accountId, performedBy);

        try {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

            if (!"Frozen".equals(account.getAccountStatus())) {
                log.warn("⚠️ Account is not frozen");
                throw new IllegalArgumentException("Account is not frozen");
            }

            account.setAccountStatus("Active");
            account.setFrozenReason(null);
            account.setFrozenAt(null);
            accountRepository.save(account);

            log.info("✅ Account status updated to Active");

            auditService.logAccountAction(performedBy, "ACCOUNT_UNFROZEN", accountId,
                    "Account " + account.getAccountNumber() + " unfrozen", request);

            notificationService.createNotification(account.getUser(), "Account Unfrozen",
                    "Your account " + account.getAccountNumber() + " has been unfrozen and is now active.",
                    "ACCOUNT");

            log.info("🟢 SUCCESS: Account unfrozen successfully: {}", account.getAccountNumber());

        } catch (Exception e) {
            log.error("🔴 ERROR in unfreezeAccount: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional
    public void closeAccount(Long accountId, String reason, String performedBy, HttpServletRequest request) {
        log.info("🔵 START: Closing account ID: {} by {}", accountId, performedBy);

        try {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

            if ("Closed".equals(account.getAccountStatus())) {
                log.warn("⚠️ Account is already closed");
                throw new IllegalArgumentException("Account is already closed");
            }

            if (account.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                log.error("❌ Cannot close account with non-zero balance");
                throw new IllegalArgumentException("Cannot close account with non-zero balance. " +
                        "Please withdraw all funds first.");
            }

            account.setAccountStatus("Closed");
            account.setClosureReason(reason);
            account.setClosedAt(LocalDateTime.now());
            accountRepository.save(account);

            log.info("✅ Account status updated to Closed");

            auditService.logAccountAction(performedBy, "ACCOUNT_CLOSED", accountId,
                    "Account " + account.getAccountNumber() + " closed. Reason: " + reason, request);

            notificationService.createNotification(account.getUser(), "Account Closed",
                    "Your account " + account.getAccountNumber() + " has been closed. Reason: " + reason,
                    "ACCOUNT");

            log.info("🟢 SUCCESS: Account closed successfully: {}", account.getAccountNumber());

        } catch (Exception e) {
            log.error("🔴 ERROR in closeAccount: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder accountNumber = new StringBuilder("AC");
        for (int i = 0; i < 10; i++) {
            accountNumber.append(random.nextInt(10));
        }

        while (accountRepository.findByAccountNumber(accountNumber.toString()).isPresent()) {
            accountNumber = new StringBuilder("AC");
            for (int i = 0; i < 10; i++) {
                accountNumber.append(random.nextInt(10));
            }
        }

        return accountNumber.toString();
    }
}