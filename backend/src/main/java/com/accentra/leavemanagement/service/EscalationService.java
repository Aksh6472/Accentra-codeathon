package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.enums.AuditAction;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.repository.AuditLogRepository;
import com.accentra.leavemanagement.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Escalates requests that have waited in PENDING_MANAGER longer than the configured timeout.
 * <p>
 * Safe to run repeatedly: each request is escalated in its own transaction after re-checking its
 * status and age; optimistic locking on the request and a unique index on ESCALATED audit rows
 * guarantee a request is escalated at most once, even if a manager acts at the same moment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EscalationService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final PolicyService policyService;
    private final LeaveTransitionService transitionService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    /** Returns the number of requests escalated in this run. */
    public int escalateOverdueRequests() {
        int timeoutMinutes = policyService.escalationTimeoutMinutes();
        Instant cutoff = clock.instant().minus(Duration.ofMinutes(timeoutMinutes));
        List<Long> candidates = leaveRequestRepository.findIdsByStatusCreatedBefore(LeaveStatus.PENDING_MANAGER, cutoff);

        int escalated = 0;
        for (Long id : candidates) {
            try {
                Boolean done = transactionTemplate.execute(tx -> escalateIfOverdue(id, cutoff, timeoutMinutes));
                if (Boolean.TRUE.equals(done)) {
                    escalated++;
                }
            } catch (ConcurrencyFailureException | DataIntegrityViolationException ex) {
                log.info("Skipped escalation of leave request {}: it changed concurrently", id);
            }
        }
        if (escalated > 0) {
            log.info("Escalated {} leave request(s) pending manager approval for more than {} minute(s)",
                    escalated, timeoutMinutes);
        }
        return escalated;
    }

    boolean escalateIfOverdue(Long requestId, Instant cutoff, int timeoutMinutes) {
        LeaveRequest request = leaveRequestRepository.findById(requestId).orElse(null);
        if (request == null
                || request.getStatus() != LeaveStatus.PENDING_MANAGER
                || !request.getCreatedAt().isBefore(cutoff)
                || auditLogRepository.existsByLeaveRequestIdAndAction(requestId, AuditAction.ESCALATED)) {
            return false;
        }
        transitionService.execute(request, LeaveAction.ESCALATE, Actor.system(),
                "No manager decision within %s; escalated to HR automatically.".formatted(describe(timeoutMinutes)));
        return true;
    }

    static String describe(int minutes) {
        if (minutes % 1440 == 0) {
            int days = minutes / 1440;
            return days == 1 ? "24 hours" : days + " days";
        }
        if (minutes % 60 == 0) {
            int hours = minutes / 60;
            return hours + (hours == 1 ? " hour" : " hours");
        }
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }
}
