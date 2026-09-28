package com.accentra.leavemanagement.dto;

import com.accentra.leavemanagement.entity.Notification;
import com.accentra.leavemanagement.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        Long leaveRequestId,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getLeaveRequest() != null ? n.getLeaveRequest().getId() : null, n.isRead(), n.getCreatedAt());
    }
}
