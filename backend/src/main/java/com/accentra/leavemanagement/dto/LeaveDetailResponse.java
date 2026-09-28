package com.accentra.leavemanagement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Full view of a request. Team conflict data is only included for managers and HR. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeaveDetailResponse(
        LeaveRequestResponse request,
        LeaveBalanceResponse balance,
        TeamConflictAnalysis teamConflicts,
        List<AuditLogResponse> history) {
}
