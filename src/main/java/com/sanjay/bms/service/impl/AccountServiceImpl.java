package com.sanjay.bms.service.impl;

import com.sanjay.bms.dto.AccountDto;
import com.sanjay.bms.entity.Account;
import com.sanjay.bms.exception.ResourceNotFoundException;
import com.sanjay.bms.mapper.AccountMapper;
import com.sanjay.bms.repository.AccountRepository;
import com.sanjay.bms.service.AccountService;
import com.sanjay.bms.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public AccountDto createAccount(AccountDto accountDto) {
        log.info("Creating account with type: {}, status: {}", accountDto.accountType(), accountDto.accountStatus());
        Account savedAccount=accountRepository.save(AccountMapper.mapToAccount(accountDto));
        log.info("Saved account - DB type: {}, DB status: {}", savedAccount.getAccountType(), savedAccount.getAccountStatus());
        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public List<AccountDto> getAllAccounts() {
        List<Account> accounts=accountRepository.findAll();
        accounts.forEach(acc -> log.info("Account {}: type={}, status={}",
                acc.getId(), acc.getAccountType(), acc.getAccountStatus()));
        return accounts.stream().
                map(AccountMapper::mapToAccountDto).
                toList();
    }

    @Override
    public AccountDto getAccount(Long id) {
        Account account=accountRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Account does not exits with id: "+id));
        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    public AccountDto updateAccount(Long id, AccountDto accountDto) {
        Account account=accountRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Account does not exits with id: "+id));
        account.setAccountNumber(accountDto.accountNumber());
        account.setAccountType(accountDto.accountType());
        account.setAccountHolderName(accountDto.accountHolderName());
        account.setBalance(accountDto.balance());
        account.setAccountStatus(accountDto.accountStatus());

        Account updatedAccount=accountRepository.save(account);
        return AccountMapper.mapToAccountDto(updatedAccount);

    }

    @Override
    public void deleteAccount(Long id) {
        Account account=accountRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Account does not exits with id: "+id));
        accountRepository.deleteById(id);
    }

    @Override
    public AccountDto findByAccountNumber(String accountNumber) {
        Account account=accountRepository.findByAccountNumber(accountNumber).orElseThrow(()->new ResourceNotFoundException("Account does not exits with account no: "+accountNumber));
        return AccountMapper.mapToAccountDto(account);

    }

    @Override
    public List<AccountDto> findByAccountTypeIgnoreCase(String accountType) {
        log.info("Searching for accounts with type: {}", accountType);
        List<Account> accountList=accountRepository.findByAccountTypeIgnoreCase(accountType);
        log.info("Found {} accounts", accountList.size());
        accountList.forEach(acc -> log.info("Found account {}: type={}, status={}",
                acc.getId(), acc.getAccountType(), acc.getAccountStatus()));
        return accountList.stream().map(AccountMapper::mapToAccountDto).toList();
    }

//    @Override
//    @Transactional
//    public AccountDto deposit(Long id, BigDecimal amount) {
//        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
//            throw new IllegalArgumentException("Deposit amount must be greater than zero");
//        }
//
//        Account account = accountRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Account does not exist with id: " + id));
//
//        BigDecimal newBalance = account.getBalance().add(amount);
//        account.setBalance(newBalance);
//
//        Account updatedAccount = accountRepository.save(account);
//        return AccountMapper.mapToAccountDto(updatedAccount);
//    }
//
//    @Override
//    @Transactional
//    public AccountDto withdraw(Long id, BigDecimal amount) {
//        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
//            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
//        }
//
//        Account account = accountRepository.findById(id)
//                .orElseThrow(() -> new ResourceNotFoundException("Account does not exist with id: " + id));
//
//        if (account.getBalance().compareTo(amount) < 0) {
//            throw new IllegalArgumentException("Insufficient balance. Available: " + account.getBalance());
//        }
//
//        BigDecimal newBalance = account.getBalance().subtract(amount);
//        account.setBalance(newBalance);
//
//        Account updatedAccount = accountRepository.save(account);
//        return AccountMapper.mapToAccountDto(updatedAccount);
//    }

    @Override
    public Long getTotalAccountCount() {
        return accountRepository.count();
    }

    @Override
    public List<AccountDto> getAccountsAboveBalance(BigDecimal minBalance) {
        List<Account> accounts = accountRepository.findByBalanceGreaterThanEqual(minBalance);
        return accounts.stream()
                .map(AccountMapper::mapToAccountDto)
                .toList();
    }


    @Autowired
    private TransactionService transactionService;

    @Override
    @Transactional
    public AccountDto deposit(Long id, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account does not exist with id: " + id));

        BigDecimal newBalance = account.getBalance().add(amount);
        account.setBalance(newBalance);

        Account updatedAccount = accountRepository.save(account);

        // Record transaction
        ((TransactionServiceImpl)transactionService).recordTransaction(
                "DEPOSIT", id, amount, newBalance, "Deposit to account"
        );

        return AccountMapper.mapToAccountDto(updatedAccount);
    }

    @Override
    @Transactional
    public AccountDto withdraw(Long id, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account does not exist with id: " + id));

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance. Available: " + account.getBalance());
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);

        Account updatedAccount = accountRepository.save(account);

        // Record transaction
        ((TransactionServiceImpl)transactionService).recordTransaction(
                "WITHDRAW", id, amount, newBalance, "Withdrawal from account"
        );

        return AccountMapper.mapToAccountDto(updatedAccount);
    }
}