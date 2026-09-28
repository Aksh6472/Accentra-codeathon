package com.accentra.leavemanagement.controller;

import com.accentra.leavemanagement.dto.NotificationResponse;
import com.accentra.leavemanagement.dto.NotificationSummaryResponse;
import com.accentra.leavemanagement.security.AuthenticatedUser;
import com.accentra.leavemanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public NotificationSummaryResponse list(@AuthenticationPrincipal AuthenticatedUser user) {
        return notificationService.listFor(user.userId());
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
        return Map.of("unreadCount", notificationService.unreadCount(user.userId()));
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return notificationService.markRead(user.userId(), id);
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal AuthenticatedUser user) {
        return Map.of("updated", notificationService.markAllRead(user.userId()));
    }
}
