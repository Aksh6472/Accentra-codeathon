package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.ApplyLeaveRequest;
import com.accentra.leavemanagement.dto.HolidayResponse;
import com.accentra.leavemanagement.dto.LeaveBalanceResponse;
import com.accentra.leavemanagement.dto.LeaveDetailResponse;
import com.accentra.leavemanagement.dto.LeavePreviewRequest;
import com.accentra.leavemanagement.dto.LeavePreviewResponse;
import com.accentra.leavemanagement.dto.LeaveRequestResponse;
import com.accentra.leavemanagement.dto.TeamConflictAnalysis;
import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.LeaveBalance;
import com.accentra.leavemanagement.entity.LeavePolicy;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.enums.AuditAction;
import com.accentra.leavemanagement.enums.LeaveAction;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.exception.BusinessRuleException;
import com.accentra.leavemanagement.exception.InvalidDateRangeException;
import com.accentra.leavemanagement.exception.InvalidStateTransitionException;
import com.accentra.leavemanagement.exception.OverlappingLeaveException;
import com.accentra.leavemanagement.exception.ResourceNotFoundException;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import com.accentra.leavemanagement.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Employee-facing leave operations: preview, apply, view, withdraw/cancel. */
@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final PolicyService policyService;
    private final LeaveApplicationValidator validator;
    private final LeaveDayCalculationService dayCalculationService;
    private final LeaveBalanceService balanceService;
    private final ConflictDetectionService conflictDetectionService;
    private final LeaveStateMachineService stateMachine;
    private final LeaveTransitionService transitionService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final LeaveAccessPolicy accessPolicy;
    private final LeaveRequestMapper mapper;
    private final Clock clock;

    /** Dry run of an application so the form can show days, balance impact and team warnings. */
    @Transactional
    public LeavePreviewResponse preview(Actor actor, LeavePreviewRequest request) {
        Employee employee = employee(actor);
        LeavePolicy policy = activePolicy(request.leaveTypeId());
        validator.validateDates(request.startDate(), request.endDate());

        LeaveDayCalculation calc = dayCalculationService.calculate(request.startDate(), request.endDate(), policy);
        LeaveBalance balance = balanceService.getOrCreate(employee, policy.getLeaveType(),
                request.startDate().getYear());
        BigDecimal remainingAfter = balance.getRemaining().subtract(BigDecimal.valueOf(calc.chargeableDays()));
        boolean overlaps = leaveRequestRepository.existsOverlapping(employee.getId(), request.startDate(),
                request.endDate(), LeaveStatus.ACTIVE);

        List<String> issues = new ArrayList<>();
        if (calc.chargeableDays() == 0) {
            issues.add("The selected dates contain no working days.");
        }
        if (remainingAfter.signum() < 0) {
            issues.add("Insufficient %s balance: %s day(s) remaining.".formatted(policy.getLeaveType().getName(),
                    balance.getRemaining().stripTrailingZeros().toPlainString()));
        }
        if (overlaps) {
            issues.add("You already have leave booked that overlaps these dates.");
        }
        if (employee.getManager() == null) {
            issues.add("No reporting manager is assigned to you. Contact HR.");
        }

        List<HolidayResponse> holidays = dayCalculationService.holidaysBetween(request.startDate(), request.endDate())
                .stream().map(HolidayResponse::from).toList();
        TeamConflictAnalysis conflicts = conflictDetectionService
                .analyze(employee, request.startDate(), request.endDate()).withoutPersonalData();

        return new LeavePreviewResponse(request.startDate(), request.endDate(), calc.calendarDays(),
                calc.chargeableDays(), calc.weekendDays(), calc.holidayDays(), holidays,
                LeaveBalanceResponse.from(balance), remainingAfter, remainingAfter.signum() >= 0, overlaps,
                conflicts, issues);
    }

    @Transactional
    public LeaveDetailResponse apply(Actor actor, ApplyLeaveRequest request) {
        Employee employee = employeeRepository.findByIdForUpdate(actor.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
        LeavePolicy policy = activePolicy(request.leaveTypeId());
        validator.validateDates(request.startDate(), request.endDate());
        if (employee.getManager() == null) {
            throw new BusinessRuleException("NO_MANAGER", "No reporting manager is assigned to you. Contact HR.");
        }

        LeaveDayCalculation calc = dayCalculationService.calculate(request.startDate(), request.endDate(), policy);
        if (calc.chargeableDays() == 0) {
            throw new InvalidDateRangeException("The selected dates contain no working days");
        }
        if (leaveRequestRepository.existsOverlapping(employee.getId(), request.startDate(), request.endDate(),
                LeaveStatus.ACTIVE)) {
            throw new OverlappingLeaveException("You already have leave booked that overlaps these dates");
        }
        LeaveBalance balance = balanceService.getOrCreate(employee, policy.getLeaveType(),
                request.startDate().getYear());
        balanceService.assertSufficient(balance, calc.chargeableDays());

        TeamConflictAnalysis conflicts = conflictDetectionService
                .analyze(employee, request.startDate(), request.endDate());

        Instant now = clock.instant();
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setLeaveType(policy.getLeaveType());
        leave.setApprover(employee.getManager());
        leave.setStartDate(request.startDate());
        leave.setEndDate(request.endDate());
        leave.setDays(calc.chargeableDays());
        leave.setReason(request.reason().trim());
        leave.setTeamAbsencePercent(conflicts.peakAbsencePercent().setScale(2, RoundingMode.HALF_UP));
        leave.setTeamLeaveWarning(conflicts.teamLeaveWarning());
        leave.setHasTeamConflict(conflicts.hasConflict());
        leave.setCreatedAt(now);
        leave.setUpdatedAt(now);
        stateMachine.initialize(leave, actor);
        leaveRequestRepository.save(leave);

        String balanceChange = balanceService.reserve(leave);
        auditService.recordTransition(leave, actor, AuditAction.CREATED, null, leave.getStatus(),
                conflicts.warningMessage());
        auditService.recordBalanceUpdate(leave, balanceChange);
        notificationService.onLeaveSubmitted(leave, conflicts);

        return new LeaveDetailResponse(mapper.toResponse(leave, actor), LeaveBalanceResponse.from(balance),
                conflicts.withoutPersonalData(), auditService.historyFor(leave.getId()));
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> myLeaves(Actor actor) {
        return leaveRequestRepository.findForEmployee(actor.employeeId()).stream()
                .map(r -> mapper.toResponse(r, actor))
                .toList();
    }

    @Transactional
    public LeaveDetailResponse details(Actor actor, Long id) {
        LeaveRequest request = find(id);
        accessPolicy.assertCanView(request, actor);
        LeaveBalance balance = balanceService.getOrCreate(request.getEmployee(), request.getLeaveType(),
                request.getStartDate().getYear());
        TeamConflictAnalysis conflicts = accessPolicy.canSeeTeamData(actor)
                ? conflictDetectionService.analyze(request.getEmployee(), request.getStartDate(), request.getEndDate())
                : null;
        return new LeaveDetailResponse(mapper.toResponse(request, actor), LeaveBalanceResponse.from(balance),
                conflicts, auditService.historyFor(id));
    }

    /**
     * Employee cancellation. Requests still awaiting a decision are withdrawn immediately; approved
     * upcoming leave moves to CANCEL_REQUESTED for the manager (or HR) to confirm.
     */
    @Transactional
    public LeaveRequestResponse cancel(Actor actor, Long id, String comment) {
        LeaveRequest request = find(id);
        accessPolicy.assertCanView(request, actor);
        LeaveAction action;
        if (LeaveStatus.AWAITING_DECISION.contains(request.getStatus())) {
            action = LeaveAction.WITHDRAW;
        } else if (request.getStatus() == LeaveStatus.APPROVED) {
            action = LeaveAction.REQUEST_CANCELLATION;
        } else {
            throw new InvalidStateTransitionException(
                    "A request that is " + request.getStatus().name().toLowerCase().replace('_', ' ')
                            + " cannot be cancelled");
        }
        transitionService.execute(request, action, actor, comment);
        return mapper.toResponse(request, actor);
    }

    private Employee employee(Actor actor) {
        return employeeRepository.findById(actor.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));
    }

    private LeavePolicy activePolicy(Long leaveTypeId) {
        LeavePolicy policy = policyService.policyForType(leaveTypeId);
        if (!policy.getLeaveType().isActive()) {
            throw new BusinessRuleException("LEAVE_TYPE_INACTIVE",
                    policy.getLeaveType().getName() + " is not currently available");
        }
        return policy;
    }

    private LeaveRequest find(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));
    }
}
