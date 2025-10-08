package com.sanjay.bms.controller;

import com.sanjay.bms.dto.*;
import com.sanjay.bms.service.TransactionService;
import com.sanjay.bms.service.StatementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;
    private final StatementService statementService;

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionDto>> getAccountTransactions(
            @PathVariable Long accountId,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(transactionService.getAccountTransactions(accountId, username));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<TransactionDto>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(transactionService.getRecentUserTransactions(username, limit));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponseDto> transferFunds(
            @RequestBody TransferRequestWithOtp request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String username = authentication.getName();
        return new ResponseEntity<>(
                transactionService.transferFunds(request, username, httpRequest),
                HttpStatus.CREATED);
    }

    @PostMapping("/transfer/initiate")
    public ResponseEntity<Map<String, String>> initiateTransfer(
            @RequestBody TransferRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String message = transactionService.initiateTransfer(request, username);
        return ResponseEntity.ok(Map.of("message", message, "requiresOtp", "true"));
    }

    // Filter transactions
    @PostMapping("/filter")
    public ResponseEntity<List<TransactionDto>> filterTransactions(
            @RequestBody TransactionFilterDto filter,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(transactionService.filterTransactions(filter, username));
    }

    // Search transactions
    @GetMapping("/search")
    public ResponseEntity<List<TransactionDto>> searchTransactions(
            @RequestParam String query,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(transactionService.searchTransactions(query, username));
    }

    // Transaction by reference number
    @GetMapping("/reference/{refNumber}")
    public ResponseEntity<TransactionDto> getTransactionByReference(
            @PathVariable String refNumber,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(transactionService.getTransactionByReference(refNumber, username));
    }

    // Download statement as PDF
    @PostMapping(value = "/statement/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public void downloadStatementPdf(
            @RequestBody StatementRequest request,
            Authentication authentication,
            HttpServletResponse response) throws IOException {
        String username = authentication.getName();
        byte[] pdfBytes = statementService.generatePdfStatement(request, username);

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=statement_" + request.getAccountId() + ".pdf");
        response.getOutputStream().write(pdfBytes);
    }

    // Download statement as CSV
    @PostMapping(value = "/statement/csv", produces = "text/csv")
    public void downloadStatementCsv(
            @RequestBody StatementRequest request,
            Authentication authentication,
            HttpServletResponse response) throws IOException {
        String username = authentication.getName();
        String csvContent = statementService.generateCsvStatement(request, username);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=statement_" + request.getAccountId() + ".csv");
        response.getWriter().write(csvContent);
    }

    // Get transaction statistics
    @GetMapping("/stats")
    public ResponseEntity<TransactionStatsDto> getTransactionStats(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate) {
        String username = authentication.getName();
        return ResponseEntity.ok(transactionService.getTransactionStats(username, startDate, endDate));
    }
}