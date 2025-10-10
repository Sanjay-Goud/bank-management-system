package com.sanjay.bms.repository;

import com.sanjay.bms.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountIdOrderByTransactionDateDesc(Long accountId);
    List<Transaction> findTop50ByOrderByTransactionDateDesc();
    List<Transaction> findByTransactionType(String transactionType);
    List<Transaction> findByStatus(String status);

    // FIXED: Added missing methods
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    List<Transaction> findByAccountIdInOrderByTransactionDateDesc(List<Long> accountIds);

    @Query("SELECT t FROM Transaction t WHERE t.accountId IN :accountIds " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "ORDER BY t.transactionDate DESC")
    List<Transaction> findByAccountIdInAndDateRange(
            @Param("accountIds") List<Long> accountIds,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate >= :startDate")
    Long countTransactionsSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByAccountIdAndDateRange(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.amount >= :minAmount " +
            "AND t.status = 'SUCCESS' ORDER BY t.transactionDate DESC")
    List<Transaction> findHighValueTransactions(@Param("minAmount") BigDecimal minAmount);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.transactionDate >= :startDate " +
            "AND t.transactionType = :type AND t.status = 'SUCCESS'")
    BigDecimal getTotalAmountByTypeAndDate(
            @Param("type") String type,
            @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate >= :startDate " +
            "AND t.transactionDate < :endDate")
    Long countTransactionsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
