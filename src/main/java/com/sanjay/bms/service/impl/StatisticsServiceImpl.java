package com.sanjay.bms.service.impl;

import com.sanjay.bms.dto.AccountStatistics;
import com.sanjay.bms.dto.InterestCalculation;
import com.sanjay.bms.entity.Account;
import com.sanjay.bms.repository.AccountRepository;
import com.sanjay.bms.service.StatisticsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final AccountRepository accountRepository;

    @Override
    public AccountStatistics getAccountStatistics() {
        List<Account> accounts = accountRepository.findAll();

        AccountStatistics stats = new AccountStatistics();
        stats.setTotalAccounts((long) accounts.size());

        if (accounts.isEmpty()) {
            stats.setTotalBalance(BigDecimal.ZERO);
            stats.setAverageBalance(BigDecimal.ZERO);
            stats.setMaxBalance(BigDecimal.ZERO);
            stats.setMinBalance(BigDecimal.ZERO);
            stats.setActiveAccounts(0L);
            stats.setInactiveAccounts(0L);
            stats.setFrozenAccounts(0L);
            stats.setAccountTypeDistribution(new HashMap<>());
            stats.setBalanceByType(new HashMap<>());
            return stats;
        }

        // Total and average balance
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalBalance(totalBalance);
        stats.setAverageBalance(totalBalance.divide(
                BigDecimal.valueOf(accounts.size()), 2, RoundingMode.HALF_UP));

        // Max and min balance
        stats.setMaxBalance(accounts.stream()
                .map(Account::getBalance)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO));
        stats.setMinBalance(accounts.stream()
                .map(Account::getBalance)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO));

        // Status counts
        stats.setActiveAccounts(accounts.stream()
                .filter(a -> "Active".equals(a.getAccountStatus()))
                .count());
        stats.setInactiveAccounts(accounts.stream()
                .filter(a -> "Inactive".equals(a.getAccountStatus()))
                .count());
        stats.setFrozenAccounts(accounts.stream()
                .filter(a -> "Frozen".equals(a.getAccountStatus()))
                .count());

        // Account type distribution
        Map<String, Long> typeDistribution = accounts.stream()
                .collect(Collectors.groupingBy(Account::getAccountType, Collectors.counting()));
        stats.setAccountTypeDistribution(typeDistribution);

        // Balance by type
        Map<String, BigDecimal> balanceByType = accounts.stream()
                .collect(Collectors.groupingBy(
                        Account::getAccountType,
                        Collectors.reducing(BigDecimal.ZERO, Account::getBalance, BigDecimal::add)
                ));
        stats.setBalanceByType(balanceByType);

        return stats;
    }

    @Override
    public InterestCalculation calculateInterest(InterestCalculation request) {
        BigDecimal principal = request.getPrincipal();
        BigDecimal rate = request.getRate();
        BigDecimal time = request.getTime();
        Integer frequency = request.getFrequency();

        // Simple Interest: SI = (P * R * T) / 100
        BigDecimal simpleInterest = principal
                .multiply(rate)
                .multiply(time)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Compound Interest: CI = P * (1 + R/(100*n))^(n*t) - P
        BigDecimal ratePerPeriod = rate.divide(
                BigDecimal.valueOf(100 * frequency), 10, RoundingMode.HALF_UP);
        BigDecimal onePlusRate = BigDecimal.ONE.add(ratePerPeriod);
        int periods = frequency * time.intValue();

        BigDecimal compoundFactor = onePlusRate.pow(periods);
        BigDecimal maturityAmount = principal.multiply(compoundFactor)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal compoundInterest = maturityAmount.subtract(principal);

        request.setSimpleInterest(simpleInterest);
        request.setCompoundInterest(compoundInterest);
        request.setMaturityAmount(maturityAmount);

        return request;
    }
}
