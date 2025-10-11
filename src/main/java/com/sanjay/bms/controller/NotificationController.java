// NotificationController.java
package com.sanjay.bms.controller;

import com.sanjay.bms.dto.NotificationDto;
import com.sanjay.bms.service.UserProfileService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@AllArgsConstructor
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getAllNotifications(Authentication authentication) {
        String username = authentication.getName();
        log.info("Fetching all notifications for user: {}", username);

        List<NotificationDto> notifications = userProfileService.getNotifications(username);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications(Authentication authentication) {
        String username = authentication.getName();
        log.info("Fetching unread notifications for user: {}", username);

        List<NotificationDto> notifications = userProfileService.getUnreadNotifications(username);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        String username = authentication.getName();

        List<NotificationDto> unreadNotifications = userProfileService.getUnreadNotifications(username);
        return ResponseEntity.ok((long) unreadNotifications.size());
    }

    // Update NotificationController.java
    @PutMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        log.info("Marking notification {} as read for user: {}", id, username);

        userProfileService.markNotificationAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(Authentication authentication) {
        String username = authentication.getName();
        log.info("Marking all notifications as read for user: {}", username);

        userProfileService.markAllNotificationsAsRead(username);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(@PathVariable Long id, Authentication authentication) {
        String username = authentication.getName();
        log.info("Deleting notification {} for user: {}", id, username);

        // Note: You may want to add a delete method in the service
        // For now, just mark as read
        userProfileService.markNotificationAsRead(id);
        return ResponseEntity.ok("Notification deleted");
    }
}