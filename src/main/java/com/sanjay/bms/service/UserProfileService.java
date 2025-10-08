package com.sanjay.bms.service;

import com.sanjay.bms.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface UserProfileService {
    UserProfileDto getUserProfile(String username);
    UserProfileDto updateProfile(String username, UserProfileDto profileDto, HttpServletRequest request);
    void changePassword(String username, ChangePasswordRequest request, HttpServletRequest httpRequest);
    TwoFactorSetupDto enableTwoFactor(String username);
    void disableTwoFactor(String username, String otpCode, HttpServletRequest request);
    boolean verifyTwoFactor(String username, String code);
    DashboardDto getUserDashboard(String username);
    List<NotificationDto> getNotifications(String username);
    List<NotificationDto> getUnreadNotifications(String username);
    void markNotificationAsRead(Long notificationId);
    void markAllNotificationsAsRead(String username);
}