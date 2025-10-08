package com.sanjay.bms.repository;

import com.sanjay.bms.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);
    List<AuditLog> findBySeverityOrderByTimestampDesc(String severity);
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM AuditLog a ORDER BY a.timestamp DESC LIMIT :limit")
    List<AuditLog> findTopNByOrderByTimestampDesc(@Param("limit") int limit);

    @Query("SELECT a FROM AuditLog a WHERE a.username = :username AND a.severity = :severity " +
            "ORDER BY a.timestamp DESC")
    List<AuditLog> findByUsernameAndSeverity(
            @Param("username") String username,
            @Param("severity") String severity);
}
