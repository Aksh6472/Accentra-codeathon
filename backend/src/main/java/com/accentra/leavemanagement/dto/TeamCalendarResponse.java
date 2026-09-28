package com.accentra.leavemanagement.dto;

import com.accentra.leavemanagement.enums.LeaveStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TeamCalendarResponse(
        TeamSummaryResponse team,
        LocalDate from,
        LocalDate to,
        BigDecimal thresholdPercent,
        List<Member> members,
        List<CalendarLeave> leaves,
        List<CalendarDay> days) {

    public record Member(Long employeeId, String fullName, String jobTitle) {
    }

    public record CalendarLeave(Long requestId, Long employeeId, String employeeName, String leaveTypeCode,
                                String leaveTypeName, LocalDate startDate, LocalDate endDate, int days,
                                LeaveStatus status) {
    }

    public record CalendarDay(LocalDate date, boolean weekend, String holidayName, int unavailableCount,
                              BigDecimal absencePercent, boolean exceedsThreshold, List<String> employeeNames) {
    }
}
