package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.enums.ActorRole;
import com.accentra.leavemanagement.security.AuthenticatedUser;

/** Whoever triggers a workflow operation: a signed-in user, or the system (scheduler). */
public record Actor(Long employeeId, String name, ActorRole role) {

    private static final Actor SYSTEM = new Actor(null, "System", ActorRole.SYSTEM);

    public static Actor of(AuthenticatedUser user) {
        return new Actor(user.employeeId(), user.fullName(), ActorRole.from(user.role()));
    }

    public static Actor system() {
        return SYSTEM;
    }

    public boolean isSystem() {
        return role == ActorRole.SYSTEM;
    }
}
