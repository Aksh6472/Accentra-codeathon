package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.entity.Notification;
import com.accentra.leavemanagement.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InAppNotificationChannel implements NotificationChannel {

    private final NotificationRepository notificationRepository;

    @Override
    public void deliver(Notification notification) {
        notificationRepository.save(notification);
    }
}
