package com.sanjay.bms.dto;

import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class FreezeAccountRequest {
    private Long accountId;
    private String reason;
}
