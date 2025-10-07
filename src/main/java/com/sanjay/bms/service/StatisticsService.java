package com.sanjay.bms.service;

import com.sanjay.bms.dto.AccountStatistics;
import com.sanjay.bms.dto.InterestCalculation;

public interface StatisticsService {
    AccountStatistics getAccountStatistics();
    InterestCalculation calculateInterest(InterestCalculation request);
}