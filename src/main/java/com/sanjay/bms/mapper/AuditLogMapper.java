package com.sanjay.bms.mapper;

import com.sanjay.bms.dto.AuditLogDto;
import com.sanjay.bms.entity.AuditLog;

public class AuditLogMapper {

    public static AuditLogDto mapToAuditLogDto(AuditLog auditLog) {
        AuditLogDto dto = new AuditLogDto();
        dto.setId(auditLog.getId());
        dto.setUsername(auditLog.getUsername());
        dto.setAction(auditLog.getAction());
        dto.setDetails(auditLog.getDetails());
        dto.setIpAddress(auditLog.getIpAddress());
        dto.setTimestamp(auditLog.getTimestamp());
        dto.setDeviceInfo(auditLog.getDeviceInfo());
        dto.setSeverity(auditLog.getSeverity());
        dto.setRelatedAccountId(auditLog.getRelatedAccountId());
        dto.setRelatedTransactionId(auditLog.getRelatedTransactionId());
        return dto;
    }
}