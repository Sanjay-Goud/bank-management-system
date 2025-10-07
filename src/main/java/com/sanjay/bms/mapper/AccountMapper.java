package com.sanjay.bms.mapper;

import com.sanjay.bms.dto.AccountDto;
import com.sanjay.bms.entity.Account;

public class AccountMapper {
    public static AccountDto mapToAccountDto(Account account){
        return new AccountDto(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getAccountType(),
                account.getBalance(),
                account.getAccountStatus()
        );
    }

    public static Account mapToAccount(AccountDto accountdto){
        return new Account(
                accountdto.id(),
                accountdto.accountNumber(),
                accountdto.accountHolderName(),
                accountdto.accountType(),
                accountdto.balance(),
                accountdto.accountStatus()
        );
    }
}