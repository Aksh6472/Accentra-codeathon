package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.LeaveBalanceResponse;
import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.LeaveBalance;
import com.accentra.leavemanagement.entity.LeavePolicy;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.entity.LeaveType;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.exception.InsufficientBalanceException;
import com.accentra.leavemanagement.exception.ResourceNotFoundException;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import com.accentra.leavemanagement.repository.LeaveBalanceRepository;
import com.accentra.leavemanagement.repository.LeavePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Owns every change to leave balances. Balances are created lazily per employee, leave type and year
 * from the database policy (pro-rated by joining date). Days move through three buckets:
 * pending (awaiting approval) → used (approved) → back to available (rejected/withdrawn/cancelled).
 */
@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final LeaveBalanceRepository balanceRepository;
    private final LeavePolicyRepository policyRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveProrationService prorationService;

    @Transactional
    public LeaveBalance getOrCreate(Employee employee, LeaveType leaveType, int year) {
        return balanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), leaveType.getId(), year)
                .orElseGet(() -> balanceRepository.save(newBalance(employee, leaveType, year)));
    }

    @Transactional
    public List<LeaveBalanceResponse> balancesFor(Long employeeId, int year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        return policyRepository.findAllActiveWithType().stream()
                .map(policy -> LeaveBalanceResponse.from(getOrCreate(employee, policy.getLeaveType(), year)))
                .toList();
    }

    public void assertSufficient(LeaveBalance balance, int days) {
        if (balance.getRemaining().compareTo(BigDecimal.valueOf(days)) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient %s balance: %s day(s) remaining, %d requested"
                            .formatted(balance.getLeaveType().getName(), balance.getRemaining().stripTrailingZeros()
                                    .toPlainString(), days));
        }
    }

    /** Holds the requested days as pending. Returns an audit description of the movement. */
    @Transactional
    public String reserve(LeaveRequest request) {
        LeaveBalance balance = balanceOf(request);
        assertSufficient(balance, request.getDays());
        balance.setPending(balance.getPending().add(days(request)));
        return "Reserved %d day(s) as pending. Remaining: %s".formatted(request.getDays(), fmt(balance.getRemaining()));
    }

    /**
     * Applies the balance effect of a state transition. Returns an audit description, or null when the
     * transition does not touch the balance. Callers run this in the same transaction as the status change,
     * so a status can only be left once and the balance can therefore only move once per transition.
     */
    @Transactional
    public String applyTransition(LeaveRequest request, LeaveAction action) {
        return switch (action) {
            case HR_APPROVE -> {
                LeaveBalance balance = balanceOf(request);
                balance.setPending(subtract(balance.getPending(), days(request)));
                balance.setUsed(balance.getUsed().add(days(request)));
                yield "Moved %d day(s) from pending to used. Remaining: %s"
                        .formatted(request.getDays(), fmt(balance.getRemaining()));
            }
            case MANAGER_REJECT, HR_REJECT, WITHDRAW -> {
                LeaveBalance balance = balanceOf(request);
                balance.setPending(subtract(balance.getPending(), days(request)));
                yield "Released %d pending day(s). Remaining: %s"
                        .formatted(request.getDays(), fmt(balance.getRemaining()));
            }
            case APPROVE_CANCELLATION -> {
                LeaveBalance balance = balanceOf(request);
                balance.setUsed(subtract(balance.getUsed(), days(request)));
                yield "Restored %d day(s) to the balance. Remaining: %s"
                        .formatted(request.getDays(), fmt(balance.getRemaining()));
            }
            case SUBMIT, MANAGER_APPROVE, ESCALATE, REQUEST_CANCELLATION, REJECT_CANCELLATION -> null;
        };
    }

    /** Re-applies a changed policy to every balance of that leave type for the given year. */
    @Transactional
    public int reallocate(LeavePolicy policy, int year) {
        List<LeaveBalance> balances = balanceRepository.findByLeaveTypeIdAndYear(policy.getLeaveType().getId(), year);
        balances.forEach(balance -> {
            balance.setAnnualEntitlement(policy.getAnnualEntitlement());
            balance.setAllocated(prorationService.proratedEntitlement(policy.getAnnualEntitlement(),
                    policy.isProrated(), balance.getEmployee().getJoiningDate(), year));
        });
        return balances.size();
    }

    private LeaveBalance balanceOf(LeaveRequest request) {
        return getOrCreate(request.getEmployee(), request.getLeaveType(), request.getStartDate().getYear());
    }

    private LeaveBalance newBalance(Employee employee, LeaveType leaveType, int year) {
        LeavePolicy policy = policyRepository.findByLeaveTypeId(leaveType.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No policy configured for " + leaveType.getName()));
        LeaveBalance balance = new LeaveBalance();
        balance.setEmployee(employee);
        balance.setLeaveType(leaveType);
        balance.setYear(year);
        balance.setAnnualEntitlement(policy.getAnnualEntitlement());
        balance.setAllocated(prorationService.proratedEntitlement(policy.getAnnualEntitlement(), policy.isProrated(),
                employee.getJoiningDate(), year));
        balance.setUsed(BigDecimal.ZERO);
        balance.setPending(BigDecimal.ZERO);
        return balance;
    }

    private static BigDecimal days(LeaveRequest request) {
        return BigDecimal.valueOf(request.getDays());
    }

    private static BigDecimal subtract(BigDecimal from, BigDecimal amount) {
        BigDecimal result = from.subtract(amount);
        if (result.signum() < 0) {
            throw new IllegalStateException("Leave balance would become negative; balance data is inconsistent");
        }
        return result;
    }

    private static String fmt(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
