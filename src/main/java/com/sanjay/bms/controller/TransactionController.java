package com.sanjay.bms.controller;

import com.sanjay.bms.dto.TransactionDto;
import com.sanjay.bms.dto.TransferRequest;
import com.sanjay.bms.service.TransactionService;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.getAllTransactions());
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionDto>> getTransactionsByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccountId(accountId));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<TransactionDto>> getRecentTransactions(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(transactionService.getRecentTransactions(limit));
    }

    @GetMapping("/today/count")
    public ResponseEntity<Long> getTodayTransactionsCount() {
        return ResponseEntity.ok(transactionService.getTodayTransactionsCount());
    }

    @GetMapping("/account/{accountId}/range")
    public ResponseEntity<List<TransactionDto>> getTransactionsByDateRange(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(transactionService.getTransactionsByDateRange(accountId, startDate, endDate));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transferFunds(@RequestBody TransferRequest transferRequest) {
        TransactionDto transaction = transactionService.transferFunds(transferRequest);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }
}