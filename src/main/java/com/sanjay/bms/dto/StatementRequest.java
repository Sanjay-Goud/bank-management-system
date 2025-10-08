package com.sanjay.bms.dto;

import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatementRequest {
    private Long accountId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String format; // PDF, CSV
}
