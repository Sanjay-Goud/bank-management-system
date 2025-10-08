package com.sanjay.bms.service.impl;


import com.sanjay.bms.dto.AccountDirectoryDto;
import com.sanjay.bms.entity.Account;
import com.sanjay.bms.exception.ResourceNotFoundException;
import com.sanjay.bms.repository.AccountRepository;
import com.sanjay.bms.service.AccountDirectoryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class AccountDirectoryServiceImpl implements AccountDirectoryService {

    private final AccountRepository accountRepository;

    @Override
    public List<AccountDirectoryDto> searchAccounts(String query) {
        List<Account> accounts;

        if (query == null || query.trim().isEmpty()) {
            accounts = accountRepository.findByAccountStatus("Active");
        } else {
            // Search by account number, holder name
            accounts = accountRepository.findAll().stream()
                    .filter(acc -> "Active".equals(acc.getAccountStatus()))
                    .filter(acc ->
                            acc.getAccountNumber().contains(query) ||
                                    acc.getAccountHolderName().toLowerCase().contains(query.toLowerCase())
                    )
                    .collect(Collectors.toList());
        }

        return accounts.stream()
                .map(this::mapToDirectoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public AccountDirectoryDto getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        if (!"Active".equals(account.getAccountStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        return mapToDirectoryDto(account);
    }

    @Override
    public List<AccountDirectoryDto> getAllActiveAccounts() {
        return accountRepository.findByAccountStatus("Active").stream()
                .map(this::mapToDirectoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountDirectoryDto> getAllAccountsDirectory() {
        List<Account> accounts = accountRepository.findAll();
        return accounts.stream()
                .map(account -> new AccountDirectoryDto(
                        account.getId(),
                        account.getAccountNumber(),
                        account.getAccountHolderName(),
                        account.getAccountType()
                ))
                .toList();
    }

    @Override
    public AccountDirectoryDto validateAccountForTransfer(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return new AccountDirectoryDto(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getAccountType()
        );
    }

    private AccountDirectoryDto mapToDirectoryDto(Account account) {
        AccountDirectoryDto dto = new AccountDirectoryDto();
        dto.setAccountNumber(account.getAccountNumber());
        dto.setMaskedAccountNumber(maskAccountNumber(account.getAccountNumber()));
        dto.setAccountHolderName(account.getAccountHolderName());
        dto.setAccountType(account.getAccountType());
        return dto;
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        String first4 = accountNumber.substring(0, 4);
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return first4 + "****" + last4;
    }
}