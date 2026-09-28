package com.accentra.leavemanagement.controller;

import com.accentra.leavemanagement.dto.CreateHolidayRequest;
import com.accentra.leavemanagement.dto.HolidayResponse;
import com.accentra.leavemanagement.security.AuthenticatedUser;
import com.accentra.leavemanagement.service.Actor;
import com.accentra.leavemanagement.service.HolidayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    public List<HolidayResponse> list(@RequestParam(required = false) Integer year) {
        return holidayService.list(year);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('HR')")
    public HolidayResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody CreateHolidayRequest request) {
        return holidayService.create(Actor.of(user), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('HR')")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        holidayService.delete(Actor.of(user), id);
    }
}
