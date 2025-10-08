package com.sanjay.bms.controller;

import com.sanjay.bms.dto.*;
import com.sanjay.bms.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<UserProfileDto> getProfile(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(userProfileService.getUserProfile(username));
    }

    @PutMapping
    public ResponseEntity<UserProfileDto> updateProfile(
            @RequestBody UserProfileDto profileDto,
            Authentication authentication,
            HttpServletRequest request) {
        String username = authentication.getName();
        return ResponseEntity.ok(userProfileService.updateProfile(username, profileDto, request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String username = authentication.getName();
        userProfileService.changePassword(username, request, httpRequest);
        return ResponseEntity.ok("Password changed successfully");
    }

    // 2FA Management
    @PostMapping("/2fa/enable")
    public ResponseEntity<TwoFactorSetupDto> enableTwoFactor(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(userProfileService.enableTwoFactor(username));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<String> disableTwoFactor(
            @RequestBody OtpVerificationRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String username = authentication.getName();
        userProfileService.disableTwoFactor(username, request.getOtpCode(), httpRequest);
        return ResponseEntity.ok("Two-factor authentication disabled");
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<String> verifyTwoFactor(
            @RequestBody OtpVerificationRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        boolean verified = userProfileService.verifyTwoFactor(username, request.getOtpCode());
        return ResponseEntity.ok(verified ? "Verified" : "Invalid OTP");
    }

    // Dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardDto> getDashboard(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(userProfileService.getUserDashboard(username));
    }

    // Notifications
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDto>> getNotifications(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(userProfileService.getNotifications(username));
    }

    @GetMapping("/notifications/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(userProfileService.getUnreadNotifications(username));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<String> markNotificationAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        userProfileService.markNotificationAsRead(id);
        return ResponseEntity.ok("Notification marked as read");
    }

    @PutMapping("/notifications/mark-all-read")
    public ResponseEntity<String> markAllNotificationsAsRead(Authentication authentication) {
        String username = authentication.getName();
        userProfileService.markAllNotificationsAsRead(username);
        return ResponseEntity.ok("All notifications marked as read");
    }
}