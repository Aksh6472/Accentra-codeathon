package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.TestData;
import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.enums.ActorRole;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.exception.BusinessRuleException;
import com.accentra.leavemanagement.exception.ForbiddenException;
import com.accentra.leavemanagement.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeaveStateMachineServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-01T09:00:00Z"), ZoneOffset.UTC);

    private final LeaveStateMachineService machine = new LeaveStateMachineService(CLOCK);
    private Employee manager;
    private Employee employee;
    private Actor employeeActor;
    private Actor managerActor;
    private final Actor hr = new Actor(99L, "Hannah HR", ActorRole.HR);

    @BeforeEach
    void setUp() {
        manager = TestData.employee(1, "Marcus Manager", null, null);
        employee = TestData.employee(2, "Asha Employee", TestData.team(1, "Eng"), manager);
        employeeActor = new Actor(2L, "Asha Employee", ActorRole.EMPLOYEE);
        managerActor = new Actor(1L, "Marcus Manager", ActorRole.MANAGER);
    }

    private LeaveRequest request(LeaveStatus status) {
        return TestData.request(10, employee, TestData.leaveType(1, "CASUAL"), LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 13), 2, status);
    }

    @Test
    void submissionStartsAtPendingManager() {
        LeaveRequest request = request(null);
        machine.initialize(request, employeeActor);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING_MANAGER);
    }

    @Test
    void managerApprovalMovesToPendingHr() {
        LeaveRequest request = request(LeaveStatus.PENDING_MANAGER);
        LeaveStatus previous = machine.transition(request, LeaveAction.MANAGER_APPROVE, managerActor, null);

        assertThat(previous).isEqualTo(LeaveStatus.PENDING_MANAGER);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING_HR);
    }

    @Test
    void hrApprovalMovesToApproved() {
        LeaveRequest request = request(LeaveStatus.PENDING_HR);
        machine.transition(request, LeaveAction.HR_APPROVE, hr, null);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED);
    }

    @Test
    void hrCanDecideEscalatedRequests() {
        LeaveRequest request = request(LeaveStatus.ESCALATED);
        machine.transition(request, LeaveAction.HR_APPROVE, hr, null);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED);
    }

    @Test
    void managerRejectionRequiresACommentAndMovesToRejected() {
        LeaveRequest request = request(LeaveStatus.PENDING_MANAGER);
        assertThatThrownBy(() -> machine.transition(request, LeaveAction.MANAGER_REJECT, managerActor, " "))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING_MANAGER);

        machine.transition(request, LeaveAction.MANAGER_REJECT, managerActor, "Release week");
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.REJECTED);
    }

    @Test
    void hrRejectionMovesToRejected() {
        LeaveRequest request = request(LeaveStatus.PENDING_HR);
        machine.transition(request, LeaveAction.HR_REJECT, hr, "Coverage gap");
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.REJECTED);
    }

    @Test
    void hrCannotSkipTheManagerStage() {
        LeaveRequest request = request(LeaveStatus.PENDING_MANAGER);
        assertThatThrownBy(() -> machine.transition(request, LeaveAction.HR_APPROVE, hr, null))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.PENDING_MANAGER);
    }

    @Test
    void managerCannotPerformHrStageApproval() {
        LeaveRequest request = request(LeaveStatus.PENDING_HR);
        assertThatThrownBy(() -> machine.transition(request, LeaveAction.HR_APPROVE, managerActor, null))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> machine.transition(request, LeaveAction.MANAGER_APPROVE, managerActor, null))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void onlyTheAssignedManagerCanDecide() {
        Actor otherManager = new Actor(55L, "Other Manager", ActorRole.MANAGER);
        assertThatThrownBy(() -> machine.transition(request(LeaveStatus.PENDING_MANAGER), LeaveAction.MANAGER_APPROVE,
                otherManager, null)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void employeesCannotApprove() {
        assertThatThrownBy(() -> machine.transition(request(LeaveStatus.PENDING_MANAGER), LeaveAction.MANAGER_APPROVE,
                employeeActor, null)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void onlyTheSystemCanEscalate() {
        assertThatThrownBy(() -> machine.transition(request(LeaveStatus.PENDING_MANAGER), LeaveAction.ESCALATE, hr, null))
                .isInstanceOf(ForbiddenException.class);
        LeaveRequest request = request(LeaveStatus.PENDING_MANAGER);
        machine.transition(request, LeaveAction.ESCALATE, Actor.system(), null);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.ESCALATED);
    }

    @ParameterizedTest
    @EnumSource(value = LeaveStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED", "PENDING_HR", "ESCALATED", "CANCEL_REQUESTED"})
    void escalationOnlyAppliesToPendingManager(LeaveStatus status) {
        assertThatThrownBy(() -> machine.transition(request(status), LeaveAction.ESCALATE, Actor.system(), null))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @ParameterizedTest
    @EnumSource(value = LeaveStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
    void finalisedRequestsCannotBeApprovedAgain(LeaveStatus status) {
        assertThatThrownBy(() -> machine.transition(request(status), LeaveAction.HR_APPROVE, hr, null))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void cancellationFlowRestoresApprovedOnDecline() {
        LeaveRequest request = request(LeaveStatus.APPROVED);
        machine.transition(request, LeaveAction.REQUEST_CANCELLATION, employeeActor, null);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.CANCEL_REQUESTED);

        machine.transition(request, LeaveAction.REJECT_CANCELLATION, managerActor, "Needed that week");
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.APPROVED);

        machine.transition(request, LeaveAction.REQUEST_CANCELLATION, employeeActor, null);
        machine.transition(request, LeaveAction.APPROVE_CANCELLATION, hr, null);
        assertThat(request.getStatus()).isEqualTo(LeaveStatus.CANCELLED);
    }

    @Test
    void leaveThatHasStartedCannotBeCancelled() {
        LeaveRequest started = request(LeaveStatus.APPROVED);
        started.setStartDate(LocalDate.of(2026, 10, 1));
        assertThatThrownBy(() -> machine.transition(started, LeaveAction.REQUEST_CANCELLATION, employeeActor, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void employeesCanOnlyCancelTheirOwnLeave() {
        Actor colleague = new Actor(3L, "Colleague", ActorRole.EMPLOYEE);
        assertThatThrownBy(() -> machine.transition(request(LeaveStatus.PENDING_MANAGER), LeaveAction.WITHDRAW,
                colleague, null)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void availableActionsReflectRoleAndState() {
        assertThat(machine.availableActions(request(LeaveStatus.PENDING_MANAGER), managerActor))
                .containsExactlyInAnyOrder(LeaveAction.MANAGER_APPROVE, LeaveAction.MANAGER_REJECT);
        assertThat(machine.availableActions(request(LeaveStatus.PENDING_MANAGER), hr)).isEmpty();
        assertThat(machine.availableActions(request(LeaveStatus.PENDING_MANAGER), employeeActor))
                .containsExactly(LeaveAction.WITHDRAW);
        assertThat(machine.availableActions(request(LeaveStatus.ESCALATED), hr))
                .containsExactlyInAnyOrder(LeaveAction.HR_APPROVE, LeaveAction.HR_REJECT);
        assertThat(machine.availableActions(request(LeaveStatus.REJECTED), employeeActor)).isEmpty();
    }
}
