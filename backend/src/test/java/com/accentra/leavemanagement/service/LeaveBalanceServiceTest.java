package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.TestData;
import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.LeaveBalance;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.entity.LeaveType;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.exception.InsufficientBalanceException;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import com.accentra.leavemanagement.repository.LeaveBalanceRepository;
import com.accentra.leavemanagement.repository.LeavePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveBalanceServiceTest {

    @Mock
    private LeaveBalanceRepository balanceRepository;
    @Mock
    private LeavePolicyRepository policyRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private LeaveBalanceService service;
    private final LeaveType earned = TestData.leaveType(1, "EARNED");
    private Employee employee;

    @BeforeEach
    void setUp() {
        service = new LeaveBalanceService(balanceRepository, policyRepository, employeeRepository,
                new LeaveProrationService());
        Employee manager = TestData.employee(1, "Manager", null, null);
        employee = TestData.employee(2, "Asha Rao", TestData.team(1, "Eng"), manager);
    }

    private LeaveBalance createdBalanceFor(LocalDate joined) {
        employee.setJoiningDate(joined);
        when(balanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(2L, 1L, 2026)).thenReturn(Optional.empty());
        when(policyRepository.findByLeaveTypeId(1L)).thenReturn(Optional.of(TestData.policy(earned, "24")));
        when(balanceRepository.save(any(LeaveBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        return service.getOrCreate(employee, earned, 2026);
    }

    @Test
    void fullYearEmployeeIsAllocatedTheFullPolicyEntitlement() {
        LeaveBalance balance = createdBalanceFor(LocalDate.of(2019, 5, 1));

        assertThat(balance.getAnnualEntitlement()).isEqualByComparingTo("24");
        assertThat(balance.getAllocated()).isEqualByComparingTo("24");
        assertThat(balance.getRemaining()).isEqualByComparingTo("24");
    }

    @Test
    void midYearEmployeeIsAllocatedAProratedEntitlementFromThePolicy() {
        LeaveBalance balance = createdBalanceFor(LocalDate.of(2026, 7, 1));

        ArgumentCaptor<LeaveBalance> saved = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(balanceRepository).save(saved.capture());
        assertThat(saved.getValue().getAnnualEntitlement()).isEqualByComparingTo("24");
        assertThat(balance.getAllocated()).isEqualByComparingTo("12");
        assertThat(balance.getRemaining()).isEqualByComparingTo("12");
    }

    @Test
    void existingBalanceIsReusedNotRecreated() {
        LeaveBalance existing = TestData.balance(employee, earned, 2026, "24", "0", "0");
        when(balanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(2L, 1L, 2026)).thenReturn(Optional.of(existing));

        assertThat(service.getOrCreate(employee, earned, 2026)).isSameAs(existing);
        verify(balanceRepository, never()).save(any());
    }

    @Test
    void zeroRemainingBalanceRejectsAnyRequest() {
        LeaveBalance exhausted = TestData.balance(employee, earned, 2026, "12", "10", "2");
        assertThat(exhausted.getRemaining()).isEqualByComparingTo("0");

        assertThatThrownBy(() -> service.assertSufficient(exhausted, 1))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("0 day(s) remaining, 1 requested");
    }

    @Test
    void exactRemainingBalanceIsSufficient() {
        LeaveBalance balance = TestData.balance(employee, earned, 2026, "12", "9", "0");
        service.assertSufficient(balance, 3);
    }

    @Test
    void balanceMovesThroughPendingUsedAndBackOnCancellation() {
        LeaveBalance balance = TestData.balance(employee, earned, 2026, "24", "0", "0");
        when(balanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(2L, 1L, 2026)).thenReturn(Optional.of(balance));
        LeaveRequest request = TestData.request(10, employee, earned, LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 7), 3, LeaveStatus.PENDING_MANAGER);

        service.reserve(request);
        assertThat(balance.getPending()).isEqualByComparingTo("3");
        assertThat(balance.getRemaining()).isEqualByComparingTo("21");

        assertThat(service.applyTransition(request, LeaveAction.MANAGER_APPROVE)).isNull();
        assertThat(balance.getPending()).isEqualByComparingTo("3");

        service.applyTransition(request, LeaveAction.HR_APPROVE);
        assertThat(balance.getPending()).isEqualByComparingTo("0");
        assertThat(balance.getUsed()).isEqualByComparingTo("3");
        assertThat(balance.getRemaining()).isEqualByComparingTo("21");

        service.applyTransition(request, LeaveAction.APPROVE_CANCELLATION);
        assertThat(balance.getUsed()).isEqualByComparingTo("0");
        assertThat(balance.getRemaining()).isEqualByComparingTo("24");
    }

    @Test
    void rejectionReleasesPendingDays() {
        LeaveBalance balance = TestData.balance(employee, earned, 2026, "24", "0", "3");
        when(balanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(2L, 1L, 2026)).thenReturn(Optional.of(balance));
        LeaveRequest request = TestData.request(10, employee, earned, LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 7), 3, LeaveStatus.REJECTED);

        service.applyTransition(request, LeaveAction.MANAGER_REJECT);

        assertThat(balance.getPending()).isEqualByComparingTo("0");
        assertThat(balance.getRemaining()).isEqualByComparingTo("24");
    }

    @Test
    void refusesToDriveABalanceNegative() {
        LeaveBalance balance = TestData.balance(employee, earned, 2026, "24", "0", "1");
        when(balanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(2L, 1L, 2026)).thenReturn(Optional.of(balance));
        LeaveRequest request = TestData.request(10, employee, earned, LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 7), 3, LeaveStatus.APPROVED);

        assertThatThrownBy(() -> service.applyTransition(request, LeaveAction.HR_APPROVE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void reservingMoreThanRemainingFails() {
        LeaveBalance balance = TestData.balance(employee, earned, 2026, "2", "0", "0");
        when(balanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(2L, 1L, 2026)).thenReturn(Optional.of(balance));
        LeaveRequest request = TestData.request(10, employee, earned, LocalDate.of(2026, 10, 5),
                LocalDate.of(2026, 10, 7), 3, LeaveStatus.PENDING_MANAGER);

        assertThatThrownBy(() -> service.reserve(request)).isInstanceOf(InsufficientBalanceException.class);
        assertThat(balance.getPending()).isEqualByComparingTo("0");
    }
}
