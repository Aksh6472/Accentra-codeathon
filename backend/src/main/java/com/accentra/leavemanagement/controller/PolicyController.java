package com.accentra.leavemanagement.controller;

import com.accentra.leavemanagement.dto.CreatePolicyRequest;
import com.accentra.leavemanagement.dto.LeavePolicyResponse;
import com.accentra.leavemanagement.dto.UpdatePolicyRequest;
import com.accentra.leavemanagement.security.AuthenticatedUser;
import com.accentra.leavemanagement.service.Actor;
import com.accentra.leavemanagement.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    /** Readable by everyone: employees need the leave types and their rules to apply. */
    @GetMapping
    public List<LeavePolicyResponse> list() {
        return policyService.listPolicies();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('HR')")
    public LeavePolicyResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody CreatePolicyRequest request) {
        return policyService.createPolicy(Actor.of(user), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public LeavePolicyResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                      @Valid @RequestBody UpdatePolicyRequest request) {
        return policyService.updatePolicy(Actor.of(user), id, request);
    }
}
