package com.accentra.leavemanagement.controller;

import com.accentra.leavemanagement.dto.DecisionRequest;
import com.accentra.leavemanagement.dto.LeaveRequestResponse;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.security.AuthenticatedUser;
import com.accentra.leavemanagement.service.Actor;
import com.accentra.leavemanagement.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.accentra.leavemanagement.controller.LeaveController.comment;

@RestController
@RequestMapping("/api/manager/leaves")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
public class ManagerController {

    private final ApprovalService approvalService;

    /** Requests awaiting this manager: new requests and cancellation requests from direct reports. */
    @GetMapping("/pending")
    public List<LeaveRequestResponse> pending(@AuthenticationPrincipal AuthenticatedUser user) {
        return approvalService.managerQueue(Actor.of(user));
    }

    @GetMapping("/escalated")
    public List<LeaveRequestResponse> escalated(@AuthenticationPrincipal AuthenticatedUser user) {
        return approvalService.managerEscalated(Actor.of(user));
    }

    @GetMapping("/history")
    public List<LeaveRequestResponse> history(@AuthenticationPrincipal AuthenticatedUser user) {
        return approvalService.managerHistory(Actor.of(user));
    }

    @PostMapping("/{id}/approve")
    public LeaveRequestResponse approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                        @Valid @RequestBody(required = false) DecisionRequest body) {
        return approvalService.decide(Actor.of(user), id, LeaveAction.MANAGER_APPROVE, comment(body));
    }

    @PostMapping("/{id}/reject")
    public LeaveRequestResponse reject(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                       @Valid @RequestBody(required = false) DecisionRequest body) {
        return approvalService.decide(Actor.of(user), id, LeaveAction.MANAGER_REJECT, comment(body));
    }

    @PostMapping("/{id}/cancellation/approve")
    public LeaveRequestResponse approveCancellation(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody(required = false) DecisionRequest body) {
        return approvalService.decide(Actor.of(user), id, LeaveAction.APPROVE_CANCELLATION, comment(body));
    }

    @PostMapping("/{id}/cancellation/reject")
    public LeaveRequestResponse rejectCancellation(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody(required = false) DecisionRequest body) {
        return approvalService.decide(Actor.of(user), id, LeaveAction.REJECT_CANCELLATION, comment(body));
    }
}
