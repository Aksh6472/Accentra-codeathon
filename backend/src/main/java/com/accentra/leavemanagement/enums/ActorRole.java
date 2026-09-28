package com.accentra.leavemanagement.enums;

/**
 * Who performed an action on a leave request. SYSTEM covers automated actions such as escalation.
 */
public enum ActorRole {
    EMPLOYEE,
    MANAGER,
    HR,
    SYSTEM;

    public static ActorRole from(Role role) {
        return ActorRole.valueOf(role.name());
    }
}
