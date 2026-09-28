package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.entity.Notification;

/**
 * A delivery mechanism for notifications. The in-app channel persists them; additional channels
 * (email, push, chat) can be added as further beans without touching the notification rules.
 */
public interface NotificationChannel {

    void deliver(Notification notification);
}
