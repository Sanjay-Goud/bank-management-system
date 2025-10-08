package com.sanjay.bms.controller;


import com.sanjay.bms.dto.*;
import com.sanjay.bms.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // User Management
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDto> getUserDetails(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserDetails(userId));
    }

    @PutMapping("/users/{userId}/enable")
    public ResponseEntity<String> enableUser(@PathVariable Long userId,
                                             HttpServletRequest request) {
        adminService.enableUser(userId, request);
        return ResponseEntity.ok("User enabled successfully");
    }

    @PutMapping("/users/{userId}/disable")
    public ResponseEntity<String> disableUser(@PathVariable Long userId,
                                              HttpServletRequest request) {
        adminService.disableUser(userId, request);
        return ResponseEntity.ok("User disabled successfully");
    }

    @PutMapping("/users/{userId}/unlock")
    public ResponseEntity<String> unlockUser(@PathVariable Long userId,
                                             HttpServletRequest request) {
        adminService.unlockUser(userId, request);
        return ResponseEntity.ok("User unlocked successfully");
    }

    // Account Management
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        return ResponseEntity.ok(adminService.getAllAccounts());
    }

    @PostMapping("/accounts/freeze")
    public ResponseEntity<String> freezeAccount(@RequestBody FreezeAccountRequest request,
                                                HttpServletRequest httpRequest) {
        adminService.freezeAccount(request.getAccountId(), request.getReason(), httpRequest);
        return ResponseEntity.ok("Account frozen successfully");
    }

    @PostMapping("/accounts/unfreeze")
    public ResponseEntity<String> unfreezeAccount(@RequestBody Map<String, Long> request,
                                                  HttpServletRequest httpRequest) {
        adminService.unfreezeAccount(request.get("accountId"), httpRequest);
        return ResponseEntity.ok("Account unfrozen successfully");
    }

    @PostMapping("/accounts/close")
    public ResponseEntity<String> closeAccount(@RequestBody CloseAccountRequest request,
                                               HttpServletRequest httpRequest) {
        adminService.closeAccount(request.getAccountId(), request.getReason(), httpRequest);
        return ResponseEntity.ok("Account closed successfully");
    }

    // Transaction Management
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDto>> getAllTransactions() {
        return ResponseEntity.ok(adminService.getAllTransactions());
    }

    @GetMapping("/transactions/pending")
    public ResponseEntity<List<TransactionDto>> getPendingTransactions() {
        return ResponseEntity.ok(adminService.getPendingTransactions());
    }

    @GetMapping("/transactions/high-value")
    public ResponseEntity<List<TransactionDto>> getHighValueTransactions() {
        return ResponseEntity.ok(adminService.getHighValueTransactions());
    }

    @PostMapping("/transactions/approve")
    public ResponseEntity<String> approveTransaction(@RequestBody TransactionApprovalRequest request,
                                                     HttpServletRequest httpRequest) {
        adminService.approveTransaction(request.getTransactionId(),
                request.getApproved(), request.getRemarks(), httpRequest);
        return ResponseEntity.ok("Transaction " +
                (request.getApproved() ? "approved" : "rejected") + " successfully");
    }

    // Dashboard Statistics
    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDto> getAdminDashboard() {
        return ResponseEntity.ok(adminService.getAdminDashboard());
    }

    // Audit Logs
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(adminService.getAuditLogs(username, severity, limit));
    }

    @GetMapping("/audit-logs/critical")
    public ResponseEntity<List<AuditLogDto>> getCriticalLogs() {
        return ResponseEntity.ok(adminService.getCriticalLogs());
    }

    // Reports
    @GetMapping("/reports/daily-summary")
    public ResponseEntity<DailySummaryDto> getDailySummary() {
        return ResponseEntity.ok(adminService.getDailySummary());
    }

    @GetMapping("/reports/account-types")
    public ResponseEntity<Map<String, Long>> getAccountTypeDistribution() {
        return ResponseEntity.ok(adminService.getAccountTypeDistribution());
    }

    @GetMapping("/reports/transaction-volume")
    public ResponseEntity<Map<String, Object>> getTransactionVolume(
            @RequestParam(required = false) String period) {
        return ResponseEntity.ok(adminService.getTransactionVolume(period));
    }
}