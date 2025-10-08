package com.sanjay.bms.controller;

import com.sanjay.bms.dto.AccountDirectoryDto;
import com.sanjay.bms.service.AccountDirectoryService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/directory")
@CrossOrigin(origins = "*")
public class AccountsDirectoryController {

    private final AccountDirectoryService directoryService;

    /**
     * Get all accounts directory (public info only)
     * Shows only account holder name and masked account number
     */
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDirectoryDto>> getAccountsDirectory(
            Authentication authentication) {
        return ResponseEntity.ok(directoryService.getAllAccountsDirectory());
    }

    /**
     * Search accounts by name or account number
     */
    @GetMapping("/accounts/search")
    public ResponseEntity<List<AccountDirectoryDto>> searchAccounts(
            @RequestParam String query,
            Authentication authentication) {
        return ResponseEntity.ok(directoryService.searchAccounts(query));
    }

    /**
     * Get account info by account number for transfer validation
     */
    @GetMapping("/accounts/validate/{accountNumber}")
    public ResponseEntity<AccountDirectoryDto> validateAccountForTransfer(
            @PathVariable String accountNumber,
            Authentication authentication) {
        return ResponseEntity.ok(directoryService.validateAccountForTransfer(accountNumber));
    }
}