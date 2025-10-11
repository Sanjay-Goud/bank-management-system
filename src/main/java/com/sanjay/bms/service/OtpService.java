package com.sanjay.bms.service;

import com.sanjay.bms.entity.Otp;
import com.sanjay.bms.entity.User;
import com.sanjay.bms.repository.OtpRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class OtpService {

    private final OtpRepository otpRepository;
    private final EmailService emailService; // ✅ Changed from JavaMailSender to EmailService
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    @Transactional
    public String generateOtp(User user, String purpose) {
        log.info("Generating OTP for user: {}, purpose: {}", user.getUsername(), purpose);

        // Invalidate previous OTPs for the same purpose
        try {
            otpRepository.invalidatePreviousOtps(user.getId(), purpose);
            log.debug("Previous OTPs invalidated for user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to invalidate previous OTPs: {}", e.getMessage(), e);
        }

        String otpCode = generateRandomOtp();
        log.debug("Generated OTP code: {} (length: {})", otpCode, otpCode.length());

        Otp otp = new Otp();
        otp.setUser(user);
        otp.setOtpCode(otpCode);
        otp.setPurpose(purpose);
        otp.setCreatedAt(LocalDateTime.now());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otp.setIsUsed(false);
        otp.setAttempts(0);

        try {
            otpRepository.save(otp);
            log.info("OTP saved to database for user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to save OTP to database: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save OTP", e);
        }

        // ✅ Send OTP via EmailService (with HTML template)
        try {
            log.info("Attempting to send OTP email to: {}", user.getEmail());
            emailService.sendOtpEmail(user.getEmail(), otpCode, purpose, OTP_EXPIRY_MINUTES);
            log.info("✅ OTP email sent successfully to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ FAILED to send OTP email to {}: {}", user.getEmail(), e.getMessage(), e);
            // Don't throw exception - OTP is already saved, user can retry
            // But log prominently so we know there's an email issue
            log.error("⚠️ EMAIL CONFIGURATION ISSUE - Check MAIL_USERNAME and MAIL_PASSWORD environment variables");
        }

        log.info("OTP generation completed for user: {}", user.getUsername());
        return otpCode;
    }

    @Transactional
    public String generateTransactionOtp(User user, String transactionRef) {
        log.info("Generating transaction OTP for user: {}, ref: {}", user.getUsername(), transactionRef);

        String otpCode = generateOtp(user, "TRANSACTION");

        // Update OTP with transaction reference
        try {
            Otp otp = otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), "TRANSACTION")
                    .orElseThrow(() -> new RuntimeException("OTP generation failed - OTP not found in database"));

            otp.setRelatedTransactionRef(transactionRef);
            otpRepository.save(otp);

            log.info("Transaction OTP linked to reference: {}", transactionRef);
        } catch (Exception e) {
            log.error("Failed to link OTP to transaction reference: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate transaction OTP", e);
        }

        return otpCode;
    }

    @Transactional
    public boolean verifyOtp(User user, String otpCode, String purpose) {
        log.info("Verifying OTP for user: {}, purpose: {}", user.getUsername(), purpose);

        Optional<Otp> otpOpt = otpRepository.findByUserAndOtpCodeAndPurpose(
                user.getId(), otpCode, purpose);

        if (otpOpt.isEmpty()) {
            log.warn("❌ Invalid OTP attempt for user: {} - OTP not found", user.getUsername());
            return false;
        }

        Otp otp = otpOpt.get();

        // Check if already used
        if (otp.getIsUsed()) {
            log.warn("❌ OTP already used for user: {}", user.getUsername());
            return false;
        }

        // Check if expired
        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            log.warn("❌ OTP expired for user: {} (expired at: {})", user.getUsername(), otp.getExpiresAt());
            return false;
        }

        // Check max attempts
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("❌ Max OTP attempts ({}) exceeded for user: {}", MAX_ATTEMPTS, user.getUsername());
            return false;
        }

        // Increment attempts
        otp.setAttempts(otp.getAttempts() + 1);

        // Mark as used if verification successful
        otp.setIsUsed(true);
        otp.setUsedAt(LocalDateTime.now());
        otpRepository.save(otp);

        log.info("✅ OTP verified successfully for user: {}", user.getUsername());
        return true;
    }

    @Transactional
    public boolean verifyTransactionOtp(User user, String otpCode, String transactionRef) {
        log.info("Verifying transaction OTP for user: {}, ref: {}", user.getUsername(), transactionRef);

        Optional<Otp> otpOpt = otpRepository.findByUserAndOtpCodeAndTransactionRef(
                user.getId(), otpCode, transactionRef);

        if (otpOpt.isEmpty()) {
            log.warn("❌ Invalid transaction OTP for user: {}, ref: {}", user.getUsername(), transactionRef);
            return false;
        }

        Otp otp = otpOpt.get();

        if (otp.getIsUsed()) {
            log.warn("❌ Transaction OTP already used for ref: {}", transactionRef);
            return false;
        }

        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            log.warn("❌ Transaction OTP expired for ref: {} (expired at: {})", transactionRef, otp.getExpiresAt());
            return false;
        }

        otp.setIsUsed(true);
        otp.setUsedAt(LocalDateTime.now());
        otpRepository.save(otp);

        log.info("✅ Transaction OTP verified successfully for ref: {}", transactionRef);
        return true;
    }

    private String generateRandomOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        String otpCode = otp.toString();
        log.debug("Generated random OTP: {} (length: {})", otpCode, otpCode.length());
        return otpCode;
    }

    @Transactional
    public void cleanupExpiredOtps() {
        log.info("Starting cleanup of expired OTPs");
        try {
            otpRepository.deleteExpiredOtps(LocalDateTime.now());
            log.info("✅ Expired OTPs cleaned up successfully");
        } catch (Exception e) {
            log.error("❌ Failed to cleanup expired OTPs: {}", e.getMessage(), e);
        }
    }
}