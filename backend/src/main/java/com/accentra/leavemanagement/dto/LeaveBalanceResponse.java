package com.accentra.leavemanagement.dto;

import com.accentra.leavemanagement.entity.LeaveBalance;

import java.math.BigDecimal;

public record LeaveBalanceResponse(
        Long leaveTypeId,
        String leaveTypeCode,
        String leaveTypeName,
        int year,
        BigDecimal annualEntitlement,
        BigDecimal allocated,
        BigDecimal used,
        BigDecimal pending,
        BigDecimal remaining) {

    public static LeaveBalanceResponse from(LeaveBalance balance) {
        return new LeaveBalanceResponse(
                balance.getLeaveType().getId(),
                balance.getLeaveType().getCode(),
                balance.getLeaveType().getName(),
                balance.getYear(),
                balance.getAnnualEntitlement(),
                balance.getAllocated(),
                balance.getUsed(),
                balance.getPending(),
                balance.getRemaining());
    }
}
