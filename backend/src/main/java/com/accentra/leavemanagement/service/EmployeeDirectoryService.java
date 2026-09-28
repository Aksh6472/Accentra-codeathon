package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.EmployeeBalanceDetailResponse;
import com.accentra.leavemanagement.dto.EmployeeDirectoryResponse;
import com.accentra.leavemanagement.dto.EmployeeProfileResponse;
import com.accentra.leavemanagement.dto.LeaveBalanceResponse;
import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.LeavePolicy;
import com.accentra.leavemanagement.exception.ResourceNotFoundException;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import com.accentra.leavemanagement.repository.LeavePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** HR-facing employee directory and the explanation of how each balance was allocated. */
@Service
@RequiredArgsConstructor
public class EmployeeDirectoryService {

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private final EmployeeRepository employeeRepository;
    private final LeavePolicyRepository policyRepository;
    private final LeaveBalanceService balanceService;
    private final LeaveProrationService prorationService;
    private final Clock clock;

    @Transactional
    public List<EmployeeDirectoryResponse> directory(Integer year) {
        int y = resolveYear(year);
        return employeeRepository.findAllForDirectory().stream()
                .map(e -> new EmployeeDirectoryResponse(EmployeeProfileResponse.from(e), y,
                        balanceService.balancesFor(e.getId(), y)))
                .toList();
    }

    @Transactional
    public EmployeeBalanceDetailResponse balanceDetail(Long employeeId, Integer year) {
        int y = resolveYear(year);
        Employee employee = employeeRepository.findByIdForDirectory(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        int months = prorationService.eligibleMonths(employee.getJoiningDate(), y);
        List<EmployeeBalanceDetailResponse.BalanceBreakdown> breakdown = policyRepository.findAllActiveWithType()
                .stream()
                .map(policy -> breakdown(employee, policy, y, months))
                .toList();
        return new EmployeeBalanceDetailResponse(EmployeeProfileResponse.from(employee), y, months,
                LeaveProrationService.MID_MONTH_CUTOFF_DAY, breakdown);
    }

    private EmployeeBalanceDetailResponse.BalanceBreakdown breakdown(Employee employee, LeavePolicy policy, int year,
                                                                     int months) {
        LeaveBalanceResponse balance = LeaveBalanceResponse.from(
                balanceService.getOrCreate(employee, policy.getLeaveType(), year));
        BigDecimal exact = policy.isProrated()
                ? balance.annualEntitlement().multiply(BigDecimal.valueOf(months))
                        .divide(MONTHS_PER_YEAR, 2, RoundingMode.HALF_UP)
                : null;
        return new EmployeeBalanceDetailResponse.BalanceBreakdown(balance, policy.isProrated(), exact);
    }

    private int resolveYear(Integer year) {
        return year != null ? year : LocalDate.now(clock).getYear();
    }
}
