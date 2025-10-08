package com.sanjay.bms.repository;

import com.sanjay.bms.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    List<Account> findByAccountTypeIgnoreCase(String accountType);
    List<Account> findByBalanceGreaterThanEqual(BigDecimal balance);
    List<Account> findByUserId(Long userId);
    List<Account> findByAccountStatus(String status);

    @Query("SELECT COUNT(a) FROM Account a WHERE a.accountStatus = :status")
    Long countByStatus(@Param("status") String status);

    @Query("SELECT SUM(a.balance) FROM Account a WHERE a.accountStatus = 'Active'")
    BigDecimal getTotalActiveBalance();
}
