package com.sanjay.bms.dto;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AccountDirectoryDto {
    private String accountNumber;
    private String maskedAccountNumber;
    private String accountHolderName;
    private String accountType;

    // FIXED: Constructor with proper field assignment
    public AccountDirectoryDto(Long id, String accountNumber, String accountHolderName, String accountType) {
        this.accountNumber = accountNumber;
        this.maskedAccountNumber = maskAccountNumber(accountNumber);
        this.accountHolderName = accountHolderName;
        this.accountType = accountType;
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        String first4 = accountNumber.substring(0, 4);
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return first4 + "****" + last4;
    }
}
