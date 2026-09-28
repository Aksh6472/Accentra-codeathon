package com.accentra.leavemanagement.dto;

import java.util.List;

public record NotificationSummaryResponse(long unreadCount, List<NotificationResponse> notifications) {
}
