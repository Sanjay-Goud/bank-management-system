package com.sanjay.bms.service;

import com.sanjay.bms.entity.AuditLog;
import com.sanjay.bms.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@AllArgsConstructor
@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logAction(String username, String action, String details,
                          HttpServletRequest request, String severity) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setDetails(details);
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setDeviceInfo(request.getHeader("User-Agent"));
        auditLog.setSeverity(severity);

        auditLogRepository.save(auditLog);
        log.info("Audit log created: {} - {} - {}", username, action, severity);
    }

    @Transactional
    public void logLogin(String username, HttpServletRequest request, boolean success) {
        String action = success ? "LOGIN_SUCCESS" : "LOGIN_FAILED";
        String severity = success ? "INFO" : "WARNING";
        String details = success ? "User logged in successfully" : "Failed login attempt";
        logAction(username, action, details, request, severity);
    }

    @Transactional
    public void logTransaction(String username, String transactionType, Long accountId,
                               String amount, HttpServletRequest request) {
        String action = "TRANSACTION_" + transactionType;
        String details = String.format("Transaction: %s, Amount: %s, Account: %d",
                transactionType, amount, accountId);
        logAction(username, action, details, request, "INFO");
    }

    @Transactional
    public void logAccountAction(String username, String action, Long accountId,
                                 String details, HttpServletRequest request) {
        String severity = action.contains("FREEZE") || action.contains("CLOSE") ? "CRITICAL" : "INFO";
        logAction(username, action, details, request, severity);
    }

    @Transactional
    public void logSecurityEvent(String username, String event, String details,
                                 HttpServletRequest request) {
        logAction(username, "SECURITY_" + event, details, request, "CRITICAL");
    }

    public List<AuditLog> getUserLogs(String username) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username);
    }

    public List<AuditLog> getCriticalLogs() {
        return auditLogRepository.findBySeverityOrderByTimestampDesc("CRITICAL");
    }

    public List<AuditLog> getRecentLogs(int limit) {
        return auditLogRepository.findTopNByOrderByTimestampDesc(limit);
    }

    public List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}