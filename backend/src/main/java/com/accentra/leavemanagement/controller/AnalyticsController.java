package com.accentra.leavemanagement.controller;

import com.accentra.leavemanagement.dto.AnalyticsResponse;
import com.accentra.leavemanagement.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasRole('HR')")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public AnalyticsResponse analytics(@RequestParam(required = false) Integer year) {
        return analyticsService.analytics(year);
    }
}
