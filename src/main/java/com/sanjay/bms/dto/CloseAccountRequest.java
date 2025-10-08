package com.sanjay.bms.dto;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CloseAccountRequest {
    private Long accountId;
    private String reason;
}
