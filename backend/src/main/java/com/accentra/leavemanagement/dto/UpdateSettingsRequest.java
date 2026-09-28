package com.accentra.leavemanagement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateSettingsRequest(
        @NotNull(message = "Escalation timeout is required")
        @Min(value = 1, message = "Escalation timeout must be at least 1 minute")
        @Max(value = 43200, message = "Escalation timeout cannot exceed 30 days")
        Integer escalationTimeoutMinutes,
        @NotNull(message = "Team absence threshold is required")
        @DecimalMin(value = "1", message = "Threshold must be at least 1%")
        @DecimalMax(value = "100", message = "Threshold cannot exceed 100%")
        BigDecimal teamAbsenceThresholdPercent) {
}
