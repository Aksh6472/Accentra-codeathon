package com.accentra.leavemanagement.dto;

import com.accentra.leavemanagement.enums.LeaveStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        int year,
        long totalRequests,
        Map<LeaveStatus, Long> requestsByStatus,
        long approvedDays,
        Utilization overallUtilization,
        List<LeaveTypeStat> leaveTypeDistribution,
        List<TeamStat> teams,
        List<MonthStat> monthlyApprovedDays) {

    public record Utilization(BigDecimal allocated, BigDecimal used, BigDecimal pending, BigDecimal utilizationPercent) {
    }

    public record LeaveTypeStat(String code, String name, long requests, long approvedDays, Utilization utilization) {
    }

    public record TeamStat(Long teamId, String name, int teamSize, int onLeaveToday, BigDecimal absencePercentToday,
                           BigDecimal peakAbsencePercentNext30Days, boolean exceedsThresholdNext30Days,
                           long approvedDays) {
    }

    public record MonthStat(int month, long approvedDays) {
    }
}
