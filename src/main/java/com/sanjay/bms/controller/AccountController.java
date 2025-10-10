package com.sanjay.bms.controller;

import com.sanjay.bms.dto.AccountDto;
import com.sanjay.bms.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@AllArgsConstructor
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@RequestBody AccountDto accountDto,
                                                    Authentication authentication,
                                                    HttpServletRequest request) {
        String username = authentication.getName();
        return new ResponseEntity<>(accountService.createAccount(accountDto, username, request),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/user")
    public ResponseEntity<List<AccountDto>> getUserAccounts(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(accountService.getUserAccounts(username));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable Long id,
                                                 Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(accountService.getAccountById(id, username));
    }

    @PutMapping("/id/{id}")
    public ResponseEntity<AccountDto> updateAccount(@PathVariable Long id,
                                                    @RequestBody AccountDto accountDto) {
        return ResponseEntity.ok(accountService.updateAccount(id, accountDto));
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
        return ResponseEntity.ok("Account deleted successfully");
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountDto> findByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.findByAccountNumber(accountNumber));
    }

    @GetMapping("/type/{accountType}")
    public ResponseEntity<List<AccountDto>> findByAccountType(@PathVariable String accountType) {
        return ResponseEntity.ok(accountService.findByAccountTypeIgnoreCase(accountType));
    }

    @PutMapping("/id/{id}/deposit")
    public ResponseEntity<AccountDto> deposit(@PathVariable Long id,
                                              @RequestBody Map<String, BigDecimal> request,
                                              Authentication authentication,
                                              HttpServletRequest httpRequest) {
        String username = authentication.getName();
        BigDecimal amount = request.get("amount");
        return ResponseEntity.ok(accountService.deposit(id, amount, username, httpRequest));
    }

    @PutMapping("/id/{id}/withdraw")
    public ResponseEntity<AccountDto> withdraw(@PathVariable Long id,
                                               @RequestBody Map<String, BigDecimal> request,
                                               Authentication authentication,
                                               HttpServletRequest httpRequest) {
        String username = authentication.getName();
        BigDecimal amount = request.get("amount");
        return ResponseEntity.ok(accountService.withdraw(id, amount, username, httpRequest));
    }

    @PutMapping("/id/{id}/freeze")
    public ResponseEntity<String> freezeAccount(@PathVariable Long id,
                                                @RequestBody Map<String, String> request,
                                                Authentication authentication,
                                                HttpServletRequest httpRequest) {
        String adminUsername = authentication.getName();
        String reason = request.get("reason");
        accountService.freezeAccount(id, reason, adminUsername, httpRequest);
        return ResponseEntity.ok("Account frozen successfully");
    }

    @PutMapping("/id/{id}/unfreeze")
    public ResponseEntity<String> unfreezeAccount(@PathVariable Long id,
                                                  Authentication authentication,
                                                  HttpServletRequest httpRequest) {
        String adminUsername = authentication.getName();
        accountService.unfreezeAccount(id, adminUsername, httpRequest);
        return ResponseEntity.ok("Account unfrozen successfully");
    }

    @PutMapping("/id/{id}/close")
    public ResponseEntity<String> closeAccount(@PathVariable Long id,
                                               @RequestBody Map<String, String> request,
                                               Authentication authentication,
                                               HttpServletRequest httpRequest) {
        String adminUsername = authentication.getName();
        String reason = request.get("reason");
        accountService.closeAccount(id, reason, adminUsername, httpRequest);
        return ResponseEntity.ok("Account closed successfully");
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getTotalAccountCount() {
        return ResponseEntity.ok(Map.of("totalAccounts", accountService.getTotalAccountCount()));
    }

    @GetMapping("/balance/above/{minBalance}")
    public ResponseEntity<List<AccountDto>> getAccountsAboveBalance(@PathVariable BigDecimal minBalance) {
        return ResponseEntity.ok(accountService.getAccountsAboveBalance(minBalance));
    }
}