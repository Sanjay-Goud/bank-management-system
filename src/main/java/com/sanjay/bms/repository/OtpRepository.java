package com.sanjay.bms.repository;

import com.sanjay.bms.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {

    @Query("SELECT o FROM Otp o WHERE o.user.id = :userId AND o.otpCode = :otpCode " +
            "AND o.purpose = :purpose AND o.isUsed = false AND o.expiresAt > CURRENT_TIMESTAMP")
    Optional<Otp> findByUserAndOtpCodeAndPurpose(
            @Param("userId") Long userId,
            @Param("otpCode") String otpCode,
            @Param("purpose") String purpose);

    @Query("SELECT o FROM Otp o WHERE o.user.id = :userId AND o.purpose = :purpose " +
            "ORDER BY o.createdAt DESC LIMIT 1")
    Optional<Otp> findLatestByUserAndPurpose(
            @Param("userId") Long userId,
            @Param("purpose") String purpose);

    @Query("SELECT o FROM Otp o WHERE o.user.id = :userId AND o.otpCode = :otpCode " +
            "AND o.relatedTransactionRef = :transactionRef AND o.isUsed = false")
    Optional<Otp> findByUserAndOtpCodeAndTransactionRef(
            @Param("userId") Long userId,
            @Param("otpCode") String otpCode,
            @Param("transactionRef") String transactionRef);

    @Modifying
    @Query("UPDATE Otp o SET o.isUsed = true WHERE o.user.id = :userId AND o.purpose = :purpose AND o.isUsed = false")
    void invalidatePreviousOtps(@Param("userId") Long userId, @Param("purpose") String purpose);

    @Modifying
    @Query("DELETE FROM Otp o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);
}