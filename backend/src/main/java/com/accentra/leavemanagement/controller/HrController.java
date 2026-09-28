package com.accentra.leavemanagement.controller;

import com.accentra.leavemanagement.dto.AuditLogResponse;
import com.accentra.leavemanagement.dto.DecisionRequest;
import com.accentra.leavemanagement.dto.EmployeeBalanceDetailResponse;
import com.accentra.leavemanagement.dto.EmployeeDirectoryResponse;
import com.accentra.leavemanagement.dto.LeaveRequestResponse;
import com.accentra.leavemanagement.dto.PageResponse;
import com.accentra.leavemanagement.enums.AuditAction;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.security.AuthenticatedUser;
import com.accentra.leavemanagement.service.Actor;
import com.accentra.leavemanagement.service.ApprovalService;
import com.accentra.leavemanagement.service.AuditService;
import com.accentra.leavemanagement.service.EmployeeDirectoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.accentra.leavemanagement.controller.LeaveController.comment;

@RestController
@RequestMapping("/api/hr")
@PreAuthorize("hasRole('HR')")
@RequiredArgsConstructor
public class HrController {

    private final ApprovalService approvalService;
    private final AuditService auditService;
    private final EmployeeDirectoryService employeeDirectoryService;

    /** Requests awaiting HR: manager-approved requests and cancellation requests. */
    @GetMapping("/leaves/pending")
    public List<LeaveRequestResponse> pending(@AuthenticationPrincipal AuthenticatedUser user) {
        return approvalService.hrQueue(Actor.of(user));
    }

    @GetMapping("/leaves/escalated")
    public List<LeaveRequestResponse> escalated(@AuthenticationPrincipal AuthenticatedUser user) {
        return approvalService.hrEscalated(Actor.of(user));
    }

    @GetMapping("/leaves")
    public List<LeaveRequestResponse> search(@AuthenticationPrincipal AuthenticatedUser user,
                                             @RequestParam(required = false) LeaveStatus status,
                                             @RequestParam(required = false) Long teamId) {
        return approvalService.search(Actor.of(user), status, teamId);
    }

    @PostMapping("/leaves/{id}/approve")
    public LeaveRequestResponse approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                        @Valid @RequestBody(required = false) DecisionRequest body) {
        return approvalService.decide(Actor.of(user), id, LeaveAction.HR_APPROVE, comment(body));
    }

    @PostMapping("/leaves/{id}/reject")
    public LeaveRequestResponse reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                       @Valid @RequestBody(required = false) DecisionRequest body) {
        return approvalService.decide(Actor.of(user), id, LeaveAction.HR_REJECT, comment(body));
    }

    @PostMapping("/leaves/{id}/cancellation/approve")
    public LeaveRequestResponse approveCancellation(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody(required = false) DecisionRequest body) {
        return approvalService.decide(Actor.of(user), id, LeaveAction.APPROVE_CANCELLATION, comment(body));
    }

    @PostMapping("/leaves/{id}/cancellation/reject")
    public LeaveRequestResponse rejectCancellation(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody(required = false) DecisionRequest body) {
        return approvalService.decide(Actor.of(user), id, LeaveAction.REJECT_CANCELLATION, comment(body));
    }

    @GetMapping("/employees")
    public List<EmployeeDirectoryResponse> employees(@RequestParam(required = false) Integer year) {
        return employeeDirectoryService.directory(year);
    }

    @GetMapping("/employees/{id}/balances")
    public EmployeeBalanceDetailResponse employeeBalances(@PathVariable Long id,
                                                          @RequestParam(required = false) Integer year) {
        return employeeDirectoryService.balanceDetail(id, year);
    }

    @GetMapping("/audit")
    public PageResponse<AuditLogResponse> audit(@RequestParam(required = false) AuditAction action,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "25") int size) {
        return auditService.search(action, page, size);
    }
}
