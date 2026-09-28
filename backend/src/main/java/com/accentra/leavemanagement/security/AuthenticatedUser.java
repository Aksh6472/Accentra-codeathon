package com.accentra.leavemanagement.security;

import com.accentra.leavemanagement.enums.Role;

/** The security principal placed in the SecurityContext for every authenticated request. */
public record AuthenticatedUser(Long userId, Long employeeId, String email, String fullName, Role role) {

    public boolean hasRole(Role expected) {
        return role == expected;
    }
}
