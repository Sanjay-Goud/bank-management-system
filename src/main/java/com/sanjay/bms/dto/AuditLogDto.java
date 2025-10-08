package com.sanjay.bms.dto;

import lombok.*;
import java.time.LocalDateTime;

// AuditLogDto.java
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogDto {
    private Long id;
    private String username;
    private String action;
    private String details;
    private String ipAddress;
    private LocalDateTime timestamp;
    private String deviceInfo;
    private String severity;
    private Long relatedAccountId;
    private Long relatedTransactionId;
}
