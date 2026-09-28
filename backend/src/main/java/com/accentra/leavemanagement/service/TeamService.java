package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.DayAbsence;
import com.accentra.leavemanagement.dto.TeamCalendarResponse;
import com.accentra.leavemanagement.dto.TeamConflictsResponse;
import com.accentra.leavemanagement.dto.TeamSummaryResponse;
import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.entity.Team;
import com.accentra.leavemanagement.enums.ActorRole;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.exception.InvalidDateRangeException;
import com.accentra.leavemanagement.exception.ResourceNotFoundException;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import com.accentra.leavemanagement.repository.LeaveRequestRepository;
import com.accentra.leavemanagement.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Team listings, the team leave calendar and team-wide conflict detection over a date range. */
@Service
@RequiredArgsConstructor
public class TeamService {

    static final int MAX_RANGE_DAYS = 93;

    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveDayCalculationService dayCalculationService;
    private final ConflictDetectionService conflictDetectionService;
    private final PolicyService policyService;
    private final LeaveAccessPolicy accessPolicy;

    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> listTeams(Actor actor) {
        List<Team> teams = actor.role() == ActorRole.HR
                ? teamRepository.findAllByOrderByName()
                : teamRepository.findByManagerIdOrderByName(actor.employeeId());
        return teams.stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public TeamCalendarResponse calendar(Actor actor, Long teamId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        Team team = accessibleTeam(actor, teamId);
        List<Employee> members = employeeRepository.findTeamMembers(teamId);
        List<LeaveRequest> leaves = leaveRequestRepository.findTeamLeavesOverlapping(teamId, from, to,
                LeaveStatus.ACTIVE);
        BigDecimal threshold = policyService.teamAbsenceThresholdPercent();
        Map<LocalDate, String> holidays = dayCalculationService.holidayNames(from, to);
        Map<LocalDate, DayAbsence> absenceByDay = conflictDetectionService
                .dailyAbsence(members.size(), dayCalculationService.workingDays(from, to), leaves, threshold, null)
                .stream().collect(Collectors.toMap(DayAbsence::date, Function.identity()));

        List<TeamCalendarResponse.CalendarDay> days = from.datesUntil(to.plusDays(1))
                .map(date -> {
                    DayAbsence absence = absenceByDay.get(date);
                    return new TeamCalendarResponse.CalendarDay(date, dayCalculationService.isWeekend(date),
                            holidays.get(date),
                            absence != null ? absence.unavailableCount() : 0,
                            absence != null ? absence.absencePercent() : BigDecimal.ZERO,
                            absence != null && absence.exceedsThreshold(),
                            absence != null ? absence.employeeNames() : List.of());
                })
                .toList();

        return new TeamCalendarResponse(summary(team), from, to, threshold,
                members.stream()
                        .map(m -> new TeamCalendarResponse.Member(m.getId(), m.getFullName(), m.getJobTitle()))
                        .toList(),
                leaves.stream()
                        .map(r -> new TeamCalendarResponse.CalendarLeave(r.getId(), r.getEmployee().getId(),
                                r.getEmployee().getFullName(), r.getLeaveType().getCode(), r.getLeaveType().getName(),
                                r.getStartDate(), r.getEndDate(), r.getDays(), r.getStatus()))
                        .toList(),
                days);
    }

    @Transactional(readOnly = true)
    public TeamConflictsResponse conflicts(Actor actor, Long teamId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        Team team = accessibleTeam(actor, teamId);
        int teamSize = (int) employeeRepository.countByTeamId(teamId);
        List<LeaveRequest> leaves = leaveRequestRepository.findTeamLeavesOverlapping(teamId, from, to,
                LeaveStatus.ACTIVE);
        BigDecimal threshold = policyService.teamAbsenceThresholdPercent();
        List<DayAbsence> conflictDays = conflictDetectionService
                .dailyAbsence(teamSize, dayCalculationService.workingDays(from, to), leaves, threshold, null).stream()
                .filter(day -> day.unavailableCount() > 1 || day.exceedsThreshold())
                .toList();
        return new TeamConflictsResponse(summary(team), from, to, threshold, conflictDays);
    }

    private Team accessibleTeam(Actor actor, Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        accessPolicy.assertCanViewTeam(team, actor);
        return team;
    }

    private TeamSummaryResponse summary(Team team) {
        Employee manager = team.getManager();
        return new TeamSummaryResponse(team.getId(), team.getName(), team.getDescription(),
                manager != null ? manager.getId() : null, manager != null ? manager.getFullName() : null,
                employeeRepository.countByTeamId(team.getId()));
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new InvalidDateRangeException("'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_RANGE_DAYS) {
            throw new InvalidDateRangeException("Date range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
    }
}
