package com.sanjay.bms.dto;

import lombok.*;

@Setter
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CloseAccountRequest {
    private Long accountId;
    private String reason;
}
