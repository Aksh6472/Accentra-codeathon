package com.accentra.leavemanagement.controller;

import com.accentra.leavemanagement.dto.EmployeeProfileResponse;
import com.accentra.leavemanagement.dto.LeaveBalanceResponse;
import com.accentra.leavemanagement.security.AuthenticatedUser;
import com.accentra.leavemanagement.service.Actor;
import com.accentra.leavemanagement.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping("/me")
    public EmployeeProfileResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return employeeService.profile(Actor.of(user));
    }

    @GetMapping("/me/balance")
    public List<LeaveBalanceResponse> myBalance(@AuthenticationPrincipal AuthenticatedUser user,
                                                @RequestParam(required = false) Integer year) {
        return employeeService.balances(Actor.of(user), year);
    }
}
