package com.sanjay.bms.controller;

import com.sanjay.bms.dto.AccountDto;
import com.sanjay.bms.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<AccountDto> createAccount(@RequestBody AccountDto accountDto){
        return new ResponseEntity<>(accountService.createAccount(accountDto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAllAccounts(){
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable Long id){
        return ResponseEntity.ok(accountService.getAccount(id));
    }

    @PutMapping("/id/{id}")
    public ResponseEntity<AccountDto> updateAccount(@PathVariable Long id,
                                                    @RequestBody AccountDto accountDto){
        return ResponseEntity.ok(accountService.updateAccount(id,accountDto));
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long id){
        accountService.deleteAccount(id);
        return ResponseEntity.ok("Account deleted successfully");
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<AccountDto> findByAccountNumber(@PathVariable String accountNumber){
        return ResponseEntity.ok(accountService.findByAccountNumber(accountNumber));
    }

    @GetMapping("/type/{accountType}")
    public ResponseEntity<List<AccountDto>> findByAccountType(@PathVariable String accountType){
        return ResponseEntity.ok(accountService.findByAccountTypeIgnoreCase(accountType));
    }

    // New endpoints

    @PutMapping("/id/{id}/deposit")
    public ResponseEntity<AccountDto> deposit(@PathVariable Long id,
                                              @RequestBody Map<String, BigDecimal> request){
        BigDecimal amount = request.get("amount");
        return ResponseEntity.ok(accountService.deposit(id, amount));
    }

    @PutMapping("/id/{id}/withdraw")
    public ResponseEntity<AccountDto> withdraw(@PathVariable Long id,
                                               @RequestBody Map<String, BigDecimal> request){
        BigDecimal amount = request.get("amount");
        return ResponseEntity.ok(accountService.withdraw(id, amount));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getTotalAccountCount(){
        return ResponseEntity.ok(Map.of("totalAccounts", accountService.getTotalAccountCount()));
    }

    @GetMapping("/balance/above/{minBalance}")
    public ResponseEntity<List<AccountDto>> getAccountsAboveBalance(@PathVariable BigDecimal minBalance){
        return ResponseEntity.ok(accountService.getAccountsAboveBalance(minBalance));
    }
}