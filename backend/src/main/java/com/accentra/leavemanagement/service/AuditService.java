package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.AuditLogResponse;
import com.accentra.leavemanagement.dto.PageResponse;
import com.accentra.leavemanagement.entity.AuditLog;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.enums.ActorRole;
import com.accentra.leavemanagement.enums.AuditAction;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.repository.AuditLogRepository;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;

    @Transactional
    public AuditLog recordTransition(LeaveRequest request, Actor actor, AuditAction action,
                                     LeaveStatus previousStatus, LeaveStatus newStatus, String comment) {
        AuditLog log = newEntry(actor, action, comment);
        log.setLeaveRequest(request);
        log.setPreviousStatus(previousStatus);
        log.setNewStatus(newStatus);
        return auditLogRepository.save(log);
    }

    /** Balance movements are always attributed to the system, since they are a consequence of a decision. */
    @Transactional
    public AuditLog recordBalanceUpdate(LeaveRequest request, String description) {
        AuditLog log = newEntry(Actor.system(), AuditAction.BALANCE_UPDATED, description);
        log.setLeaveRequest(request);
        log.setPreviousStatus(request.getStatus());
        log.setNewStatus(request.getStatus());
        return auditLogRepository.save(log);
    }

    @Transactional
    public AuditLog recordConfigurationChange(Actor actor, AuditAction action, String description) {
        return auditLogRepository.save(newEntry(actor, action, description));
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> historyFor(Long leaveRequestId) {
        return auditLogRepository.findByLeaveRequestIdOrderByCreatedAtAscIdAsc(leaveRequestId).stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(AuditAction action, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<AuditLog> result = action == null
                ? auditLogRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)
                : auditLogRepository.findByActionOrderByCreatedAtDescIdDesc(action, pageable);
        return PageResponse.from(result, AuditLogResponse::from);
    }

    private AuditLog newEntry(Actor actor, AuditAction action, String comment) {
        AuditLog log = new AuditLog();
        if (actor.employeeId() != null) {
            log.setActor(employeeRepository.getReferenceById(actor.employeeId()));
        }
        log.setActorName(actor.name());
        log.setActorRole(actor.role() != null ? actor.role() : ActorRole.SYSTEM);
        log.setAction(action);
        log.setComment(comment);
        log.setCreatedAt(clock.instant());
        return log;
    }
}
