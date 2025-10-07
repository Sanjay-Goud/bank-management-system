package com.sanjay.bms.controller;

import com.sanjay.bms.dto.AccountStatistics;
import com.sanjay.bms.dto.InterestCalculation;
import com.sanjay.bms.service.StatisticsService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/accounts")
    public ResponseEntity<AccountStatistics> getAccountStatistics() {
        return ResponseEntity.ok(statisticsService.getAccountStatistics());
    }

    @PostMapping("/calculate-interest")
    public ResponseEntity<InterestCalculation> calculateInterest(
            @RequestBody InterestCalculation request) {
        return ResponseEntity.ok(statisticsService.calculateInterest(request));
    }
}