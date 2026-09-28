package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.LeaveRequestResponse;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.exception.ResourceNotFoundException;
import com.accentra.leavemanagement.repository.LeaveRequestRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Manager and HR approval queues and decisions. */
@Service
@RequiredArgsConstructor
public class ApprovalService {

    private static final Set<LeaveStatus> MANAGER_QUEUE = EnumSet.of(LeaveStatus.PENDING_MANAGER,
            LeaveStatus.CANCEL_REQUESTED);
    private static final Set<LeaveStatus> DECIDED = EnumSet.of(LeaveStatus.PENDING_HR, LeaveStatus.APPROVED,
            LeaveStatus.REJECTED, LeaveStatus.CANCELLED);
    private static final Set<LeaveStatus> HR_QUEUE = EnumSet.of(LeaveStatus.PENDING_HR,
            LeaveStatus.CANCEL_REQUESTED);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTransitionService transitionService;
    private final LeaveRequestMapper mapper;

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> managerQueue(Actor manager) {
        return map(leaveRequestRepository.findForApprover(manager.employeeId(), MANAGER_QUEUE), manager);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> managerEscalated(Actor manager) {
        return map(leaveRequestRepository.findForApprover(manager.employeeId(), EnumSet.of(LeaveStatus.ESCALATED)),
                manager);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> managerHistory(Actor manager) {
        return leaveRequestRepository.findForApprover(manager.employeeId(), DECIDED).stream()
                .sorted(Comparator.comparing(LeaveRequest::getUpdatedAt).reversed())
                .map(r -> mapper.toResponse(r, manager))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> hrQueue(Actor hr) {
        return map(leaveRequestRepository.findByStatuses(HR_QUEUE), hr);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> hrEscalated(Actor hr) {
        return map(leaveRequestRepository.findByStatuses(EnumSet.of(LeaveStatus.ESCALATED)), hr);
    }

    /** Organisation-wide view for HR, optionally filtered by status and team. */
    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> search(Actor hr, LeaveStatus status, Long teamId) {
        Specification<LeaveRequest> spec = (root, query, cb) -> {
            if (Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("employee", JoinType.INNER);
                root.fetch("leaveType", JoinType.INNER);
            }
            return cb.conjunction();
        };
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (teamId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("employee").get("team").get("id"), teamId));
        }
        return leaveRequestRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(r -> mapper.toResponse(r, hr))
                .toList();
    }

    @Transactional
    public LeaveRequestResponse decide(Actor actor, Long requestId, LeaveAction action, String comment) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
        transitionService.execute(request, action, actor, comment);
        return mapper.toResponse(request, actor);
    }

    private List<LeaveRequestResponse> map(List<LeaveRequest> requests, Actor viewer) {
        return requests.stream().map(r -> mapper.toResponse(r, viewer)).toList();
    }
}
