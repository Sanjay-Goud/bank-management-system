package com.sanjay.bms.mapper;

import com.sanjay.bms.dto.TransactionDto;
import com.sanjay.bms.entity.Transaction;

public class TransactionMapper {

    public static TransactionDto mapToTransactionDto(Transaction transaction) {
        TransactionDto dto = new TransactionDto();
        dto.setId(transaction.getId());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setAccountId(transaction.getAccountId());
        dto.setAmount(transaction.getAmount());
        dto.setBalanceAfter(transaction.getBalanceAfter());
        dto.setDescription(transaction.getDescription());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setReferenceNumber(transaction.getReferenceNumber());
        dto.setToAccountId(transaction.getToAccountId());
        dto.setStatus(transaction.getStatus());
        return dto;
    }

    public static Transaction mapToTransaction(TransactionDto dto) {
        Transaction transaction = new Transaction();
        transaction.setId(dto.getId());
        transaction.setTransactionType(dto.getTransactionType());
        transaction.setAccountId(dto.getAccountId());
        transaction.setAmount(dto.getAmount());
        transaction.setBalanceAfter(dto.getBalanceAfter());
        transaction.setDescription(dto.getDescription());
        transaction.setTransactionDate(dto.getTransactionDate());
        transaction.setReferenceNumber(dto.getReferenceNumber());
        transaction.setToAccountId(dto.getToAccountId());
        transaction.setStatus(dto.getStatus());
        return transaction;
    }
}