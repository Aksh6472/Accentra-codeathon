package com.accentra.leavemanagement.dto;

import com.accentra.leavemanagement.entity.WorkflowSettings;

import java.math.BigDecimal;
import java.time.Instant;

public record WorkflowSettingsResponse(int escalationTimeoutMinutes, BigDecimal teamAbsenceThresholdPercent,
                                       Instant updatedAt) {

    public static WorkflowSettingsResponse from(WorkflowSettings settings) {
        return new WorkflowSettingsResponse(settings.getEscalationTimeoutMinutes(),
                settings.getTeamAbsenceThresholdPercent(), settings.getUpdatedAt());
    }
}
