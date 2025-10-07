package com.sanjay.bms.repository;

import com.sanjay.bms.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByAccountTypeIgnoreCase(String accountType);

    List<Account> findByBalanceGreaterThanEqual(BigDecimal balance);
}
