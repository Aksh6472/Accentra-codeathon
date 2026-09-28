package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.EmployeeProfileResponse;
import com.accentra.leavemanagement.dto.LeaveBalanceResponse;
import com.accentra.leavemanagement.exception.ResourceNotFoundException;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final LeaveBalanceService balanceService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public EmployeeProfileResponse profile(Actor actor) {
        return employeeRepository.findById(actor.employeeId())
                .map(EmployeeProfileResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
    }

    @Transactional
    public List<LeaveBalanceResponse> balances(Actor actor, Integer year) {
        return balanceService.balancesFor(actor.employeeId(), year != null ? year : LocalDate.now(clock).getYear());
    }
}
