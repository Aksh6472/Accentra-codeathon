package com.accentra.leavemanagement.dto;

import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.enums.LeaveStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * A leave request as seen by a particular viewer. {@code availableActions} lists the transitions
 * that viewer may perform right now; the frontend renders buttons from it, the backend re-validates.
 */
public record LeaveRequestResponse(
        Long id,
        Long employeeId,
        String employeeName,
        String employeeCode,
        Long teamId,
        String teamName,
        Long leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        int days,
        String reason,
        LeaveStatus status,
        BigDecimal teamAbsencePercent,
        boolean teamLeaveWarning,
        boolean hasTeamConflict,
        Long approverId,
        String approverName,
        Instant createdAt,
        Instant updatedAt,
        Instant escalatedAt,
        Set<LeaveAction> availableActions) {
}
