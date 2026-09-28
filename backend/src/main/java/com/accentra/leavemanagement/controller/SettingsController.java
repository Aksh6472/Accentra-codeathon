package com.accentra.leavemanagement.controller;

import com.accentra.leavemanagement.dto.UpdateSettingsRequest;
import com.accentra.leavemanagement.dto.WorkflowSettingsResponse;
import com.accentra.leavemanagement.security.AuthenticatedUser;
import com.accentra.leavemanagement.service.Actor;
import com.accentra.leavemanagement.service.PolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasRole('HR')")
@RequiredArgsConstructor
public class SettingsController {

    private final PolicyService policyService;

    @GetMapping
    public WorkflowSettingsResponse get() {
        return policyService.getSettings();
    }

    @PutMapping
    public WorkflowSettingsResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                           @Valid @RequestBody UpdateSettingsRequest request) {
        return policyService.updateSettings(Actor.of(user), request);
    }
}
