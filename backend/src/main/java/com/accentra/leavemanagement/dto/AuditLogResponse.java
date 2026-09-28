package com.accentra.leavemanagement.dto;

import com.accentra.leavemanagement.entity.AuditLog;
import com.accentra.leavemanagement.enums.ActorRole;
import com.accentra.leavemanagement.enums.AuditAction;
import com.accentra.leavemanagement.enums.LeaveStatus;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        Long leaveRequestId,
        String actorName,
        ActorRole actorRole,
        AuditAction action,
        LeaveStatus previousStatus,
        LeaveStatus newStatus,
        String comment,
        Instant createdAt) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getLeaveRequest() != null ? log.getLeaveRequest().getId() : null,
                log.getActorName(),
                log.getActorRole(),
                log.getAction(),
                log.getPreviousStatus(),
                log.getNewStatus(),
                log.getComment(),
                log.getCreatedAt());
    }
}
