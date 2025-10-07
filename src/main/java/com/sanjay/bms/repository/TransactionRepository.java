package com.sanjay.bms.repository;

import com.sanjay.bms.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountIdOrderByTransactionDateDesc(Long accountId);

    List<Transaction> findTop50ByOrderByTransactionDateDesc();

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate >= :startDate")
    Long countTransactionsSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    List<Transaction> findByTransactionType(String transactionType);
}