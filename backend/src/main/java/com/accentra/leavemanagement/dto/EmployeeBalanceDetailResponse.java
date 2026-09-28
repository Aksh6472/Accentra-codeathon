package com.accentra.leavemanagement.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * An employee's balances for a year with the inputs that produced each allocation, so HR can see
 * exactly how a pro-rated entitlement was derived.
 */
public record EmployeeBalanceDetailResponse(
        EmployeeProfileResponse profile,
        int year,
        int eligibleMonths,
        int midMonthCutoffDay,
        List<BalanceBreakdown> balances) {

    /**
     * @param exactProrated annual × eligibleMonths / 12 before rounding (null when the policy is not pro-rated)
     */
    public record BalanceBreakdown(LeaveBalanceResponse balance, boolean prorated, BigDecimal exactProrated) {
    }
}
