package com.accentra.leavemanagement.controller;

import com.accentra.leavemanagement.dto.ApplyLeaveRequest;
import com.accentra.leavemanagement.dto.DecisionRequest;
import com.accentra.leavemanagement.dto.LeaveDetailResponse;
import com.accentra.leavemanagement.dto.LeavePreviewRequest;
import com.accentra.leavemanagement.dto.LeavePreviewResponse;
import com.accentra.leavemanagement.dto.LeaveRequestResponse;
import com.accentra.leavemanagement.security.AuthenticatedUser;
import com.accentra.leavemanagement.service.Actor;
import com.accentra.leavemanagement.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('EMPLOYEE')")
    public LeaveDetailResponse apply(@AuthenticationPrincipal AuthenticatedUser user,
                                     @Valid @RequestBody ApplyLeaveRequest request) {
        return leaveService.apply(Actor.of(user), request);
    }

    @PostMapping("/preview")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public LeavePreviewResponse preview(@AuthenticationPrincipal AuthenticatedUser user,
                                        @Valid @RequestBody LeavePreviewRequest request) {
        return leaveService.preview(Actor.of(user), request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<LeaveRequestResponse> myLeaves(@AuthenticationPrincipal AuthenticatedUser user) {
        return leaveService.myLeaves(Actor.of(user));
    }

    /** Owner, the assigned manager and HR may view a request; access is checked per record. */
    @GetMapping("/{id}")
    public LeaveDetailResponse details(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return leaveService.details(Actor.of(user), id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public LeaveRequestResponse cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                       @Valid @RequestBody(required = false) DecisionRequest body) {
        return leaveService.cancel(Actor.of(user), id, comment(body));
    }

    static String comment(DecisionRequest body) {
        return body != null ? body.comment() : null;
    }
}
