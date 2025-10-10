package com.sanjay.bms.service;

import com.sanjay.bms.entity.Otp;
import com.sanjay.bms.entity.User;
import com.sanjay.bms.repository.OtpRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final JavaMailSender mailSender;
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    @Transactional
    public String generateOtp(User user, String purpose) {
        // Invalidate previous OTPs for the same purpose
        otpRepository.invalidatePreviousOtps(user.getId(), purpose);

        String otpCode = generateRandomOtp();

        Otp otp = new Otp();
        otp.setUser(user);
        otp.setOtpCode(otpCode);
        otp.setPurpose(purpose);
        otp.setCreatedAt(LocalDateTime.now());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otp.setIsUsed(false);
        otp.setAttempts(0);

        otpRepository.save(otp);

        // Send OTP via email
        sendOtpEmail(user.getEmail(), otpCode, purpose);

        log.info("OTP generated for user {} for purpose {}", user.getUsername(), purpose);
        return otpCode;
    }

    @Transactional
    public String generateTransactionOtp(User user, String transactionRef) {
        String otpCode = generateOtp(user, "TRANSACTION");

        // Update OTP with transaction reference
        Otp otp = otpRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), "TRANSACTION")
                .orElseThrow(() -> new RuntimeException("OTP generation failed"));
        otp.setRelatedTransactionRef(transactionRef);
        otpRepository.save(otp);

        return otpCode;
    }

    @Transactional
    public boolean verifyOtp(User user, String otpCode, String purpose) {
        Optional<Otp> otpOpt = otpRepository.findByUserAndOtpCodeAndPurpose(
                user.getId(), otpCode, purpose);

        if (otpOpt.isEmpty()) {
            log.warn("Invalid OTP attempt for user {}", user.getUsername());
            return false;
        }

        Otp otp = otpOpt.get();

        // Check if already used
        if (otp.getIsUsed()) {
            log.warn("OTP already used for user {}", user.getUsername());
            return false;
        }

        // Check if expired
        if (LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            log.warn("OTP expired for user {}", user.getUsername());
            return false;
        }

        // Check max attempts
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("Max OTP attempts exceeded for user {}", user.getUsername());
            return false;
        }

        // Increment attempts
        otp.setAttempts(otp.getAttempts() + 1);

        // Mark as used if verification successful
        otp.setIsUsed(true);
        otp.setUsedAt(LocalDateTime.now());
        otpRepository.save(otp);

        log.info("OTP verified successfully for user {}", user.getUsername());
        return true;
    }

    @Transactional
    public boolean verifyTransactionOtp(User user, String otpCode, String transactionRef) {
        Optional<Otp> otpOpt = otpRepository.findByUserAndOtpCodeAndTransactionRef(
                user.getId(), otpCode, transactionRef);

        if (otpOpt.isEmpty()) {
            return false;
        }

        Otp otp = otpOpt.get();

        if (otp.getIsUsed() || LocalDateTime.now().isAfter(otp.getExpiresAt())) {
            return false;
        }

        otp.setIsUsed(true);
        otp.setUsedAt(LocalDateTime.now());
        otpRepository.save(otp);

        return true;
    }

    private String generateRandomOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private void sendOtpEmail(String email, String otpCode, String purpose) {
        try {
            String subject = getEmailSubject(purpose);
            String text = String.format(
                    "Your OTP for %s is: %s\n\n" +
                            "This OTP will expire in %d minutes.\n" +
                            "Do not share this OTP with anyone.\n\n" +
                            "If you did not request this, please contact support immediately.",
                    purpose, otpCode, OTP_EXPIRY_MINUTES
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("noreply@bankingapp.com");
            mailSender.send(message);

            log.info("OTP email sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
        }
    }

    private String getEmailSubject(String purpose) {
        return switch (purpose) {
            case "LOGIN_2FA" -> "Login OTP - Banking App";
            case "TRANSACTION" -> "Transaction OTP - Banking App";
            case "PASSWORD_RESET" -> "Password Reset OTP - Banking App";
            default -> "OTP Verification - Banking App";
        };
    }

    @Transactional
    public void cleanupExpiredOtps() {
        otpRepository.deleteExpiredOtps(LocalDateTime.now());
        log.info("Expired OTPs cleaned up");
    }
}