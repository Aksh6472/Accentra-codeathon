package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.DayAbsence;
import com.accentra.leavemanagement.dto.TeamConflictAnalysis;
import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.entity.Team;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import com.accentra.leavemanagement.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Measures how a leave request affects team availability. The result is advisory for the approving
 * manager: a conflict or excessive absence never rejects a request automatically.
 * <p>
 * For each working day in the requested range, absence % = (team members already on active leave
 * + the applicant) / team size × 100. Active leave covers pending, escalated, approved and
 * cancel-requested requests. The team leave warning is raised when any day's absence % is strictly
 * greater than the configured threshold.
 */
@Service
@RequiredArgsConstructor
public class ConflictDetectionService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveDayCalculationService dayCalculationService;
    private final PolicyService policyService;

    @Transactional(readOnly = true)
    public TeamConflictAnalysis analyze(Employee applicant, LocalDate start, LocalDate end) {
        BigDecimal threshold = policyService.teamAbsenceThresholdPercent();
        Team team = applicant.getTeam();
        if (team == null) {
            return TeamConflictAnalysis.noTeam(threshold);
        }
        int teamSize = (int) employeeRepository.countByTeamId(team.getId());
        List<LeaveRequest> teammatesLeave = leaveRequestRepository
                .findTeamLeavesOverlapping(team.getId(), start, end, LeaveStatus.ACTIVE).stream()
                .filter(r -> !r.getEmployee().getId().equals(applicant.getId()))
                .toList();
        List<LocalDate> workingDays = dayCalculationService.workingDays(start, end);
        return buildAnalysis(team, teamSize, applicant, workingDays, teammatesLeave, threshold);
    }

    TeamConflictAnalysis buildAnalysis(Team team, int teamSize, Employee applicant, List<LocalDate> workingDays,
                                       List<LeaveRequest> teammatesLeave, BigDecimal threshold) {
        List<DayAbsence> perDay = dailyAbsence(teamSize, workingDays, teammatesLeave, threshold, applicant);

        DayAbsence peak = perDay.stream()
                .max(Comparator.comparingInt(DayAbsence::unavailableCount)
                        .thenComparing(DayAbsence::date, Comparator.reverseOrder()))
                .orElse(null);
        List<DayAbsence> affected = perDay.stream()
                .filter(day -> day.unavailableCount() > 1 || day.exceedsThreshold())
                .toList();
        List<DayAbsence> overThreshold = perDay.stream().filter(DayAbsence::exceedsThreshold).toList();
        boolean hasConflict = perDay.stream().anyMatch(day -> day.unavailableCount() > 1);
        boolean warning = !overThreshold.isEmpty();

        List<TeamConflictAnalysis.OverlappingLeave> overlapping = teammatesLeave.stream()
                .filter(r -> workingDays.stream().anyMatch(day -> r.overlaps(day, day)))
                .map(r -> new TeamConflictAnalysis.OverlappingLeave(r.getId(), r.getEmployee().getId(),
                        r.getEmployee().getFullName(), r.getLeaveType().getCode(), r.getLeaveType().getName(),
                        r.getStartDate(), r.getEndDate(), r.getStatus().name()))
                .toList();

        return new TeamConflictAnalysis(
                team.getId(),
                team.getName(),
                teamSize,
                peak != null ? peak.unavailableCount() : 0,
                peak != null ? peak.absencePercent() : BigDecimal.ZERO,
                peak != null ? peak.date() : null,
                threshold,
                hasConflict,
                warning,
                warningMessage(overThreshold, hasConflict, overlapping.size(), affected.size(), threshold),
                affected,
                overlapping);
    }

    /**
     * Absence for each given day. {@code extraAbsentee} (nullable) is counted as absent on every day,
     * which is how a not-yet-approved request is evaluated.
     */
    public List<DayAbsence> dailyAbsence(int teamSize, List<LocalDate> days, List<LeaveRequest> leaves,
                                         BigDecimal threshold, Employee extraAbsentee) {
        List<DayAbsence> result = new ArrayList<>(days.size());
        for (LocalDate day : days) {
            Set<String> names = new LinkedHashSet<>();
            Set<Long> ids = new LinkedHashSet<>();
            for (LeaveRequest leave : leaves) {
                if (leave.overlaps(day, day) && ids.add(leave.getEmployee().getId())) {
                    names.add(leave.getEmployee().getFullName());
                }
            }
            if (extraAbsentee != null && ids.add(extraAbsentee.getId())) {
                names.add(extraAbsentee.getFullName());
            }
            BigDecimal percent = absencePercent(ids.size(), teamSize);
            result.add(new DayAbsence(day, ids.size(), teamSize, percent, percent.compareTo(threshold) > 0,
                    List.copyOf(names)));
        }
        return result;
    }

    public static BigDecimal absencePercent(int unavailable, int teamSize) {
        if (teamSize <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY);
        }
        return BigDecimal.valueOf(unavailable).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(teamSize), 1, RoundingMode.HALF_UP);
    }

    private static String warningMessage(List<DayAbsence> overThreshold, boolean hasConflict, int overlappingCount,
                                         int affectedDays, BigDecimal threshold) {
        if (!overThreshold.isEmpty()) {
            DayAbsence worst = overThreshold.stream()
                    .max(Comparator.comparing(DayAbsence::absencePercent)).orElseThrow();
            String others = overThreshold.size() > 1
                    ? " (and %d other day%s)".formatted(overThreshold.size() - 1, overThreshold.size() == 2 ? "" : "s")
                    : "";
            return "High team absence: %s%% of the team (%d of %d) is unavailable on %s%s. Threshold is %s%%."
                    .formatted(worst.absencePercent().stripTrailingZeros().toPlainString(), worst.unavailableCount(),
                            worst.teamSize(), DATE.format(worst.date()), others,
                            threshold.stripTrailingZeros().toPlainString());
        }
        if (hasConflict) {
            return "Overlaps with %d team member%s on %d working day%s."
                    .formatted(overlappingCount, overlappingCount == 1 ? "" : "s", affectedDays,
                            affectedDays == 1 ? "" : "s");
        }
        return null;
    }
}
