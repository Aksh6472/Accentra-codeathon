package com.accentra.leavemanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Team availability impact of a leave request. Purely advisory: it never blocks or rejects a request.
 */
public record TeamConflictAnalysis(
        Long teamId,
        String teamName,
        int teamSize,
        int peakUnavailable,
        BigDecimal peakAbsencePercent,
        LocalDate peakDate,
        BigDecimal thresholdPercent,
        boolean hasConflict,
        boolean teamLeaveWarning,
        String warningMessage,
        List<DayAbsence> affectedDates,
        List<OverlappingLeave> overlappingLeaves) {

    public static TeamConflictAnalysis noTeam(BigDecimal threshold) {
        return new TeamConflictAnalysis(null, null, 0, 0, BigDecimal.ZERO, null, threshold, false, false,
                null, List.of(), List.of());
    }

    /** Aggregate-only view for employees: keeps counts and percentages, drops colleagues' names and leave. */
    public TeamConflictAnalysis withoutPersonalData() {
        List<DayAbsence> days = affectedDates.stream()
                .map(d -> new DayAbsence(d.date(), d.unavailableCount(), d.teamSize(), d.absencePercent(),
                        d.exceedsThreshold(), List.of()))
                .toList();
        return new TeamConflictAnalysis(teamId, teamName, teamSize, peakUnavailable, peakAbsencePercent, peakDate,
                thresholdPercent, hasConflict, teamLeaveWarning, warningMessage, days, List.of());
    }

    public record OverlappingLeave(
            Long requestId,
            Long employeeId,
            String employeeName,
            String leaveTypeCode,
            String leaveTypeName,
            LocalDate startDate,
            LocalDate endDate,
            String status) {
    }
}
