package com.sanjay.bms.mapper;

import com.sanjay.bms.dto.AccountDto;
import com.sanjay.bms.entity.Account;

public class AccountMapper {

    public static AccountDto mapToAccountDto(Account account) {
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
        dto.setUserId(account.getUser() != null ? account.getUser().getId() : null); // ✅ Fixed here
        return dto;
    }

    public static Account mapToAccount(AccountDto accountDto) {
        Account account = new Account();
        account.setId(accountDto.getId());
        account.setAccountNumber(accountDto.getAccountNumber());
        account.setAccountHolderName(accountDto.getAccountHolderName());
        account.setAccountType(accountDto.getAccountType());
        account.setBalance(accountDto.getBalance());
        account.setAccountStatus(accountDto.getAccountStatus());
        account.setCreatedAt(accountDto.getCreatedAt());
        account.setLastTransactionDate(accountDto.getLastTransactionDate());
        account.setDailyTransactionLimit(accountDto.getDailyTransactionLimit());
        account.setPerTransactionLimit(accountDto.getPerTransactionLimit());
        account.setInterestRate(accountDto.getInterestRate());
        account.setMinimumBalance(accountDto.getMinimumBalance());
        return account;
    }

    private static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        String first4 = accountNumber.substring(0, 4);
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return first4 + "****" + last4;
    }
}
