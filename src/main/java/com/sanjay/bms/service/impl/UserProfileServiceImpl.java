package com.sanjay.bms.service.impl;

import com.sanjay.bms.dto.*;
import com.sanjay.bms.entity.Account;
import com.sanjay.bms.entity.Notification;
import com.sanjay.bms.entity.User;
import com.sanjay.bms.exception.ResourceNotFoundException;
import com.sanjay.bms.mapper.AccountMapper;
import com.sanjay.bms.mapper.NotificationMapper;
import com.sanjay.bms.mapper.UserMapper;
import com.sanjay.bms.repository.AccountRepository;
import com.sanjay.bms.repository.NotificationRepository;
import com.sanjay.bms.repository.TransactionRepository;
import com.sanjay.bms.repository.UserRepository;
import com.sanjay.bms.security.PasswordValidator;
import com.sanjay.bms.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final TwoFactorAuthService twoFactorAuthService;
    private final AuditService auditService;

    @Override
    public UserProfileDto getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserProfileDto dto = UserMapper.mapToUserProfileDto(user);

        // Get user accounts
        List<Account> accounts = accountRepository.findByUser_Id(user.getId());
        dto.setAccounts(accounts.stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList()));

        dto.setTotalAccounts(accounts.size());
        dto.setTotalBalance(accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return dto;
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(String username, UserProfileDto profileDto,
                                        HttpServletRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update allowed fields
        if (profileDto.getFullName() != null) {
            user.setFullName(profileDto.getFullName());
        }
        if (profileDto.getPhoneNumber() != null) {
            user.setPhoneNumber(profileDto.getPhoneNumber());
        }
        if (profileDto.getAddress() != null) {
            user.setAddress(profileDto.getAddress());
        }
        if (profileDto.getDateOfBirth() != null) {
            user.setDateOfBirth(profileDto.getDateOfBirth());
        }

        User updatedUser = userRepository.save(user);

        // Audit log
        auditService.logAction(username, "PROFILE_UPDATED",
                "User profile updated", request, "INFO");

        return getUserProfile(username);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request,
                               HttpServletRequest httpRequest) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Validate new password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        PasswordValidator.ValidationResult validation =
                passwordValidator.validate(request.getNewPassword());
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getErrorMessage());
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Audit log
        auditService.logSecurityEvent(username, "PASSWORD_CHANGED",
                "User changed password", httpRequest);

        log.info("Password changed for user: {}", username);
    }

    @Override
    @Transactional
    public TwoFactorSetupDto enableTwoFactor(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getTwoFactorEnabled()) {
            throw new IllegalArgumentException("Two-factor authentication is already enabled");
        }

        // Generate secret
        String secret = twoFactorAuthService.generateSecretKey();
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        // Generate QR code
        String qrCodeUrl = twoFactorAuthService.generateQRCodeUrl(username, secret);
        String qrCodeBase64 = twoFactorAuthService.generateQRCodeBase64(qrCodeUrl);

        // Generate backup codes
        String[] backupCodes = twoFactorAuthService.generateBackupCodes(8);

        TwoFactorSetupDto dto = new TwoFactorSetupDto();
        dto.setSecret(secret);
        dto.setQrCodeUrl("data:image/png;base64," + qrCodeBase64);
        dto.setBackupCodes(String.join(", ", backupCodes));

        log.info("2FA enabled for user: {}", username);
        return dto;
    }

    @Override
    @Transactional
    public void disableTwoFactor(String username, String otpCode, HttpServletRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getTwoFactorEnabled()) {
            throw new IllegalArgumentException("Two-factor authentication is not enabled");
        }

        // Verify OTP before disabling
        if (!twoFactorAuthService.verifyCode(user.getTwoFactorSecret(), otpCode)) {
            throw new IllegalArgumentException("Invalid OTP code");
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);

        // Audit log
        auditService.logSecurityEvent(username, "2FA_DISABLED",
                "Two-factor authentication disabled", request);

        log.info("2FA disabled for user: {}", username);
    }

    @Override
    public boolean verifyTwoFactor(String username, String code) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getTwoFactorEnabled()) {
            return false;
        }

        return twoFactorAuthService.verifyCode(user.getTwoFactorSecret(), code);
    }

    // Replace the getUserDashboard method with this corrected version:

    @Override
    public DashboardDto getUserDashboard(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DashboardDto dashboard = new DashboardDto();

        // Get accounts
        List<Account> accounts = accountRepository.findByUser_Id(user.getId());
        List<AccountDto> accountDtos = accounts.stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList());
        dashboard.setAccounts(accountDtos);

        // Calculate totals
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTotalBalance(totalBalance);
        dashboard.setTotalAccounts(accounts.size());

        int activeAccounts = (int) accounts.stream()
                .filter(a -> "Active".equals(a.getAccountStatus()))
                .count();
        dashboard.setActiveAccounts(activeAccounts);

        // Today's transactions count
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        Long todayTxns = transactionRepository.countTransactionsSince(startOfDay);
        dashboard.setTodayTransactions(todayTxns);

        // Recent transactions (last 10) - FIXED
        List<TransactionDto> recentTxns = accounts.stream()
                .flatMap(acc -> transactionRepository
                        .findByAccountIdOrderByTransactionDateDesc(acc.getId()).stream())  // ✅ Now uses acc.getId() which returns account ID
                .limit(10)
                .map(com.sanjay.bms.mapper.TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());
        dashboard.setRecentTransactions(recentTxns);

        // Unread notifications
        List<Notification> unreadNotifs = notificationRepository
                .findByUserAndIsReadFalseOrderByCreatedAtDesc(user);
        dashboard.setUnreadNotifications(unreadNotifs.stream()
                .map(NotificationMapper::mapToNotificationDto)
                .collect(Collectors.toList()));

        return dashboard;
    }

    @Override
    public List<NotificationDto> getNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(NotificationMapper::mapToNotificationDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationDto> getUnreadNotifications(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return notificationRepository.findByUserAndIsReadFalseOrderByCreatedAtDesc(user).stream()
                .map(NotificationMapper::mapToNotificationDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markNotificationAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllNotificationsAsRead(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Notification> notifications = notificationRepository
                .findByUserAndIsReadFalse(user);

        notifications.forEach(n -> {
            n.setIsRead(true);
            n.setReadAt(LocalDateTime.now());
        });

        notificationRepository.saveAll(notifications);
    }
}