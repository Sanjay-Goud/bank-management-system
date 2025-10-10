package com.sanjay.bms.service;

import com.sanjay.bms.dto.AccountDto;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    // ✅ UPDATED: Added username and request parameters
    AccountDto createAccount(AccountDto accountDto, String username, HttpServletRequest request);

    List<AccountDto> getAllAccounts();

    // ✅ NEW: Get user's accounts
    List<AccountDto> getUserAccounts(String username);

    // ✅ UPDATED: Added username parameter for security
    AccountDto getAccountById(Long id, String username);

    // Keep original method for backward compatibility
    AccountDto getAccount(Long id);

    AccountDto updateAccount(Long id, AccountDto accountDto);

    void deleteAccount(Long id);

    AccountDto findByAccountNumber(String accountNumber);

    List<AccountDto> findByAccountTypeIgnoreCase(String accountType);

    // ✅ UPDATED: Added username and request parameters
    AccountDto deposit(Long id, BigDecimal amount, String username, HttpServletRequest request);

    // ✅ UPDATED: Added username and request parameters
    AccountDto withdraw(Long id, BigDecimal amount, String username, HttpServletRequest request);

    Long getTotalAccountCount();

    List<AccountDto> getAccountsAboveBalance(BigDecimal minBalance);

    // Admin operations
    void freezeAccount(Long accountId, String reason, String performedBy, HttpServletRequest request);

    void unfreezeAccount(Long accountId, String performedBy, HttpServletRequest request);

    void closeAccount(Long accountId, String reason, String performedBy, HttpServletRequest request);
}