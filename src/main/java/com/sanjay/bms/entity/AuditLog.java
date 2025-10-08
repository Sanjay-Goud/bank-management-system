package com.sanjay.bms.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String action; // LOGIN, LOGOUT, TRANSFER, DEPOSIT, WITHDRAW, ACCOUNT_FROZEN, etc.

    @Column(length = 1000)
    private String details;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String deviceInfo;

    private String severity; // INFO, WARNING, CRITICAL

    private Long relatedAccountId;

    private Long relatedTransactionId;
}