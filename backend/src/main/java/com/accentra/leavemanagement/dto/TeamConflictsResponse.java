package com.accentra.leavemanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Working days in a range where two or more team members overlap, or absence exceeds the threshold. */
public record TeamConflictsResponse(
        TeamSummaryResponse team,
        LocalDate from,
        LocalDate to,
        BigDecimal thresholdPercent,
        List<DayAbsence> conflictDays) {
}
