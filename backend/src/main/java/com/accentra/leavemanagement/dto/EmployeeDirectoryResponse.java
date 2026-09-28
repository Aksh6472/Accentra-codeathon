package com.accentra.leavemanagement.dto;

import java.util.List;

/** One row of the HR employee directory: the profile plus every active leave balance for the year. */
public record EmployeeDirectoryResponse(
        EmployeeProfileResponse profile,
        int year,
        List<LeaveBalanceResponse> balances) {
}
