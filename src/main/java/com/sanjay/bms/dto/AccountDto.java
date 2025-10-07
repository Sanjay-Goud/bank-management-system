package com.sanjay.bms.dto;

import java.math.BigDecimal;

public record AccountDto(Long id,
                         String accountNumber,
                         String accountHolderName,
                         String accountType,
                         BigDecimal balance,
                         String accountStatus
){}
