package com.accentra.leavemanagement.enums;

import java.util.EnumSet;
import java.util.Set;

public enum LeaveStatus {
    PENDING_MANAGER,
    PENDING_HR,
    APPROVED,
    REJECTED,
    ESCALATED,
    CANCEL_REQUESTED,
    CANCELLED;

    /** Requests that still reserve the employee's dates (block overlaps and count towards team absence). */
    public static final Set<LeaveStatus> ACTIVE =
            EnumSet.of(PENDING_MANAGER, PENDING_HR, ESCALATED, APPROVED, CANCEL_REQUESTED);

    /** Requests still awaiting an approval decision; their days are held as "pending" balance. */
    public static final Set<LeaveStatus> AWAITING_DECISION = EnumSet.of(PENDING_MANAGER, PENDING_HR, ESCALATED);

    /** Requests whose days have been deducted as "used" balance. */
    public static final Set<LeaveStatus> CONSUMING = EnumSet.of(APPROVED, CANCEL_REQUESTED);
}
