package com.sanjay.bms.mapper;

import com.sanjay.bms.dto.TransactionDto;
import com.sanjay.bms.entity.Transaction;

public class TransactionMapper {
    public static TransactionDto mapToTransactionDto(Transaction transaction) {
        return new TransactionDto(
                transaction.getId(),
                transaction.getTransactionType(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getReferenceNumber(),
                transaction.getToAccountId(),
                transaction.getStatus()
        );
    }

    public static Transaction mapToTransaction(TransactionDto transactionDto) {
        return new Transaction(
                transactionDto.getId(),
                transactionDto.getTransactionType(),
                transactionDto.getAccountId(),
                transactionDto.getAmount(),
                transactionDto.getBalanceAfter(),
                transactionDto.getDescription(),
                transactionDto.getTransactionDate(),
                transactionDto.getReferenceNumber(),
                transactionDto.getToAccountId(),
                transactionDto.getStatus()
        );
    }
}