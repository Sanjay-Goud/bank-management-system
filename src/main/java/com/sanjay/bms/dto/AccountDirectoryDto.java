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


    public AccountDirectoryDto(Long id, String accountNumber, String accountHolderName, String accountType) {
    }
}