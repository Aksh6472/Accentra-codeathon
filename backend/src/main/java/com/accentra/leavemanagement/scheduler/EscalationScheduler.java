package com.accentra.leavemanagement.scheduler;

import com.accentra.leavemanagement.service.EscalationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically escalates overdue manager approvals. Interval: app.escalation.check-interval-ms. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscalationScheduler {

    private final EscalationService escalationService;

    @Scheduled(fixedDelayString = "${app.escalation.check-interval-ms}",
            initialDelayString = "${app.escalation.initial-delay-ms}")
    public void escalateOverdueRequests() {
        try {
            escalationService.escalateOverdueRequests();
        } catch (RuntimeException ex) {
            // Never let one failed run stop future runs.
            log.error("Escalation run failed", ex);
        }
    }
}
