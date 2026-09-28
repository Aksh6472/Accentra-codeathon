package com.accentra.leavemanagement.dto;

import com.accentra.leavemanagement.entity.LeavePolicy;

import java.math.BigDecimal;
import java.time.Instant;

public record LeavePolicyResponse(
        Long id,
        Long leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        String description,
        boolean active,
        BigDecimal annualEntitlement,
        boolean prorated,
        boolean countWeekends,
        boolean countHolidays,
        Instant updatedAt) {

    public static LeavePolicyResponse from(LeavePolicy policy) {
        var type = policy.getLeaveType();
        return new LeavePolicyResponse(policy.getId(), type.getId(), type.getCode(), type.getName(),
                type.getDescription(), type.isActive(), policy.getAnnualEntitlement(), policy.isProrated(),
                policy.isCountWeekends(), policy.isCountHolidays(), policy.getUpdatedAt());
    }
}
