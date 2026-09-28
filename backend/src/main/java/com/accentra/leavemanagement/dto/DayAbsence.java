package com.accentra.leavemanagement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DayAbsence(
        LocalDate date,
        int unavailableCount,
        int teamSize,
        BigDecimal absencePercent,
        boolean exceedsThreshold,
        List<String> employeeNames) {
}
