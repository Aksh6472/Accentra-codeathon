package com.accentra.leavemanagement.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LeavePreviewRequest(
        @NotNull(message = "Leave type is required") Long leaveTypeId,
        @NotNull(message = "Start date is required") LocalDate startDate,
        @NotNull(message = "End date is required") LocalDate endDate) {
}
