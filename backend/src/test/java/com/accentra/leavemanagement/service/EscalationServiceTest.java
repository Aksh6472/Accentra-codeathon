package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.TestData;
import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.enums.AuditAction;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.repository.AuditLogRepository;
import com.accentra.leavemanagement.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final int TIMEOUT_MINUTES = 24 * 60;
    private static final Instant CUTOFF = NOW.minus(Duration.ofMinutes(TIMEOUT_MINUTES));

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private PolicyService policyService;
    @Mock
    private LeaveTransitionService transitionService;

    private EscalationService service;
    private Employee employee;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new EscalationService(leaveRequestRepository, auditLogRepository, policyService, transitionService,
                tx, Clock.fixed(NOW, ZoneOffset.UTC));
        employee = TestData.employee(2, "Asha", TestData.team(1, "Eng"), TestData.employee(1, "Boss", null, null));
    }

    private LeaveRequest pendingSince(long id, Instant createdAt, LeaveStatus status) {
        LeaveRequest request = TestData.request(id, employee, TestData.leaveType(1, "CASUAL"),
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 20), 1, status);
        request.setCreatedAt(createdAt);
        return request;
    }

    @Test
    void requestWithinTimeoutIsNotEscalated() {
        when(policyService.escalationTimeoutMinutes()).thenReturn(TIMEOUT_MINUTES);
        when(leaveRequestRepository.findIdsByStatusCreatedBefore(LeaveStatus.PENDING_MANAGER, CUTOFF)).thenReturn(List.of());

        assertThat(service.escalateOverdueRequests()).isZero();
        verify(transitionService, never()).execute(any(), any(), any(), any());
    }

    @Test
    void requestBeyondTimeoutIsEscalatedBySystem() {
        LeaveRequest overdue = pendingSince(5, NOW.minus(Duration.ofHours(30)), LeaveStatus.PENDING_MANAGER);
        when(policyService.escalationTimeoutMinutes()).thenReturn(TIMEOUT_MINUTES);
        when(leaveRequestRepository.findIdsByStatusCreatedBefore(LeaveStatus.PENDING_MANAGER, CUTOFF)).thenReturn(List.of(5L));
        when(leaveRequestRepository.findById(5L)).thenReturn(Optional.of(overdue));

        assertThat(service.escalateOverdueRequests()).isEqualTo(1);
        verify(transitionService).execute(eq(overdue), eq(LeaveAction.ESCALATE), eq(Actor.system()), anyString());
    }

    @Test
    void alreadyEscalatedRequestIsNotEscalatedAgain() {
        LeaveRequest escalated = pendingSince(5, NOW.minus(Duration.ofHours(30)), LeaveStatus.ESCALATED);
        when(leaveRequestRepository.findById(5L)).thenReturn(Optional.of(escalated));

        assertThat(service.escalateIfOverdue(5L, CUTOFF, TIMEOUT_MINUTES)).isFalse();
        verify(transitionService, never()).execute(any(), any(), any(), any());
    }

    @Test
    void requestWithExistingEscalationRecordIsSkipped() {
        LeaveRequest request = pendingSince(5, NOW.minus(Duration.ofHours(30)), LeaveStatus.PENDING_MANAGER);
        when(leaveRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(auditLogRepository.existsByLeaveRequestIdAndAction(5L, AuditAction.ESCALATED)).thenReturn(true);

        assertThat(service.escalateIfOverdue(5L, CUTOFF, TIMEOUT_MINUTES)).isFalse();
        verify(transitionService, never()).execute(any(), any(), any(), any());
    }

    @Test
    void requestApprovedMeanwhileIsSkipped() {
        LeaveRequest decided = pendingSince(5, NOW.minus(Duration.ofHours(30)), LeaveStatus.PENDING_HR);
        when(leaveRequestRepository.findById(5L)).thenReturn(Optional.of(decided));

        assertThat(service.escalateIfOverdue(5L, CUTOFF, TIMEOUT_MINUTES)).isFalse();
    }

    @Test
    void recentRequestIsNotEscalatedEvenIfSelected() {
        LeaveRequest fresh = pendingSince(5, NOW.minus(Duration.ofHours(2)), LeaveStatus.PENDING_MANAGER);
        when(leaveRequestRepository.findById(5L)).thenReturn(Optional.of(fresh));

        assertThat(service.escalateIfOverdue(5L, CUTOFF, TIMEOUT_MINUTES)).isFalse();
    }

    @Test
    void concurrentModificationSkipsOnlyThatRequest() {
        LeaveRequest first = pendingSince(5, NOW.minus(Duration.ofHours(30)), LeaveStatus.PENDING_MANAGER);
        LeaveRequest second = pendingSince(6, NOW.minus(Duration.ofHours(30)), LeaveStatus.PENDING_MANAGER);
        when(policyService.escalationTimeoutMinutes()).thenReturn(TIMEOUT_MINUTES);
        when(leaveRequestRepository.findIdsByStatusCreatedBefore(LeaveStatus.PENDING_MANAGER, CUTOFF))
                .thenReturn(List.of(5L, 6L));
        when(leaveRequestRepository.findById(5L)).thenReturn(Optional.of(first));
        when(leaveRequestRepository.findById(6L)).thenReturn(Optional.of(second));
        when(transitionService.execute(eq(first), any(), any(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(LeaveRequest.class, 5L));

        assertThat(service.escalateOverdueRequests()).isEqualTo(1);
        verify(transitionService).execute(eq(second), eq(LeaveAction.ESCALATE), any(), any());
    }

    @Test
    void describesTimeoutReadably() {
        assertThat(EscalationService.describe(1440)).isEqualTo("24 hours");
        assertThat(EscalationService.describe(2880)).isEqualTo("2 days");
        assertThat(EscalationService.describe(120)).isEqualTo("2 hours");
        assertThat(EscalationService.describe(1)).isEqualTo("1 minute");
    }
}
