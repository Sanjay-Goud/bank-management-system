package com.sanjay.bms.dto;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OtpVerificationRequest {
    private String otpCode;
    private String purpose;
}
