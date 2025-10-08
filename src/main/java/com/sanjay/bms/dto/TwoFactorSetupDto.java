package com.sanjay.bms.dto;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TwoFactorSetupDto {
    private String secret;
    private String qrCodeUrl;
    private String backupCodes;
}
