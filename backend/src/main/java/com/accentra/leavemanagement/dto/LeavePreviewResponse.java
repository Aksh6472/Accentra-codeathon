package com.accentra.leavemanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LeavePreviewResponse(
        LocalDate startDate,
        LocalDate endDate,
        int calendarDays,
        int chargeableDays,
        int weekendDays,
        int holidayDays,
        List<HolidayResponse> holidays,
        LeaveBalanceResponse balance,
        BigDecimal remainingAfter,
        boolean sufficientBalance,
        boolean overlapsExistingLeave,
        TeamConflictAnalysis teamConflicts,
        List<String> blockingIssues) {
}
