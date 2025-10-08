package com.sanjay.bms.mapper;

import com.sanjay.bms.dto.NotificationDto;
import com.sanjay.bms.entity.Notification;

public class NotificationMapper {

    public static NotificationDto mapToNotificationDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        dto.setRelatedTransactionId(notification.getRelatedTransactionId());
        dto.setRelatedAccountId(notification.getRelatedAccountId());
        return dto;
    }
}