package com.accentra.leavemanagement.enums;

/**
 * Every operation that can move a leave request between states. The permitted
 * source states and actor roles for each action live in LeaveStateMachineService.
 */
public enum LeaveAction {
    SUBMIT(AuditAction.CREATED),
    MANAGER_APPROVE(AuditAction.MANAGER_APPROVED),
    MANAGER_REJECT(AuditAction.MANAGER_REJECTED),
    HR_APPROVE(AuditAction.HR_APPROVED),
    HR_REJECT(AuditAction.HR_REJECTED),
    ESCALATE(AuditAction.ESCALATED),
    WITHDRAW(AuditAction.WITHDRAWN),
    REQUEST_CANCELLATION(AuditAction.CANCELLATION_REQUESTED),
    APPROVE_CANCELLATION(AuditAction.CANCELLATION_APPROVED),
    REJECT_CANCELLATION(AuditAction.CANCELLATION_REJECTED);

    private final AuditAction auditAction;

    LeaveAction(AuditAction auditAction) {
        this.auditAction = auditAction;
    }

    public AuditAction auditAction() {
        return auditAction;
    }
}
