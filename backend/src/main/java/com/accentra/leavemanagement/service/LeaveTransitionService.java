package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.enums.LeaveStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Executes a state transition and all of its consequences atomically: status change (via the state
 * machine), balance movement, audit trail and notifications. Every workflow path — employee
 * cancellation, manager/HR decisions, automatic escalation — goes through here.
 */
@Service
@RequiredArgsConstructor
public class LeaveTransitionService {

    private final LeaveStateMachineService stateMachine;
    private final LeaveBalanceService balanceService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final Clock clock;

    @Transactional
    public LeaveRequest execute(LeaveRequest request, LeaveAction action, Actor actor, String comment) {
        String normalizedComment = comment == null || comment.isBlank() ? null : comment.trim();
        LeaveStatus previous = stateMachine.transition(request, action, actor, normalizedComment);

        Instant now = clock.instant();
        request.setUpdatedAt(now);
        if (action == LeaveAction.ESCALATE) {
            request.setEscalatedAt(now);
        }

        String balanceChange = balanceService.applyTransition(request, action);
        auditService.recordTransition(request, actor, action.auditAction(), previous, request.getStatus(),
                normalizedComment);
        if (balanceChange != null) {
            auditService.recordBalanceUpdate(request, balanceChange);
        }
        notificationService.onTransition(request, action, actor, normalizedComment);
        return request;
    }
}
