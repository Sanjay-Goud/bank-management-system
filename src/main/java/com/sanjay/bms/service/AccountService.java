package com.sanjay.bms.service;

import com.sanjay.bms.dto.AccountDto;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    AccountDto createAccount(AccountDto accountDto);

    List<AccountDto> getAllAccounts();

    AccountDto getAccount(Long id);

    AccountDto updateAccount(Long id, AccountDto accountDto);

    void deleteAccount(Long id);

    AccountDto findByAccountNumber(String accountNumber);

    List<AccountDto> findByAccountTypeIgnoreCase(String accountType);

    AccountDto deposit(Long id, BigDecimal amount);

    AccountDto withdraw(Long id, BigDecimal amount);

    Long getTotalAccountCount();

    List<AccountDto> getAccountsAboveBalance(BigDecimal minBalance);
}
