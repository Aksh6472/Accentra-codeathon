package com.accentra.leavemanagement.controller;

import com.accentra.leavemanagement.dto.TeamCalendarResponse;
import com.accentra.leavemanagement.dto.TeamConflictsResponse;
import com.accentra.leavemanagement.dto.TeamSummaryResponse;
import com.accentra.leavemanagement.security.AuthenticatedUser;
import com.accentra.leavemanagement.service.Actor;
import com.accentra.leavemanagement.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
@PreAuthorize("hasAnyRole('MANAGER', 'HR')")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public List<TeamSummaryResponse> teams(@AuthenticationPrincipal AuthenticatedUser user) {
        return teamService.listTeams(Actor.of(user));
    }

    @GetMapping("/{id}/calendar")
    public TeamCalendarResponse calendar(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return teamService.calendar(Actor.of(user), id, from, to);
    }

    @GetMapping("/{id}/conflicts")
    public TeamConflictsResponse conflicts(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return teamService.conflicts(Actor.of(user), id, from, to);
    }
}
