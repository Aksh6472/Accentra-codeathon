package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.dto.AnalyticsResponse;
import com.accentra.leavemanagement.dto.DayAbsence;
import com.accentra.leavemanagement.entity.LeaveBalance;
import com.accentra.leavemanagement.entity.LeavePolicy;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.entity.Team;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import com.accentra.leavemanagement.repository.LeaveBalanceRepository;
import com.accentra.leavemanagement.repository.LeavePolicyRepository;
import com.accentra.leavemanagement.repository.LeaveRequestRepository;
import com.accentra.leavemanagement.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Organisation leave analytics for a calendar year. Requests are attributed to the year and month in
 * which they start; utilisation is used days ÷ allocated days from leave balances.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int LOOKAHEAD_DAYS = 30;

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final LeavePolicyRepository policyRepository;
    private final TeamRepository teamRepository;
    private final EmployeeRepository employeeRepository;
    private final ConflictDetectionService conflictDetectionService;
    private final LeaveDayCalculationService dayCalculationService;
    private final PolicyService policyService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(Integer requestedYear) {
        LocalDate today = LocalDate.now(clock);
        int year = requestedYear != null ? requestedYear : today.getYear();
        List<LeaveRequest> requests = leaveRequestRepository
                .findOverlappingRange(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)).stream()
                .filter(r -> r.getStartDate().getYear() == year)
                .toList();
        List<LeaveBalance> balances = balanceRepository.findAllForYear(year);

        Map<LeaveStatus, Long> byStatus = new EnumMap<>(LeaveStatus.class);
        for (LeaveStatus status : LeaveStatus.values()) {
            byStatus.put(status, requests.stream().filter(r -> r.getStatus() == status).count());
        }
        Predicate<LeaveRequest> consuming = r -> LeaveStatus.CONSUMING.contains(r.getStatus());
        long approvedDays = requests.stream().filter(consuming).mapToLong(LeaveRequest::getDays).sum();

        List<AnalyticsResponse.LeaveTypeStat> byType = policyRepository.findAllWithType().stream()
                .map(LeavePolicy::getLeaveType)
                .map(type -> {
                    Predicate<LeaveRequest> ofType = r -> r.getLeaveType().getId().equals(type.getId());
                    return new AnalyticsResponse.LeaveTypeStat(type.getCode(), type.getName(),
                            requests.stream().filter(ofType).count(),
                            requests.stream().filter(consuming.and(ofType)).mapToLong(LeaveRequest::getDays).sum(),
                            utilization(balances.stream()
                                    .filter(b -> b.getLeaveType().getId().equals(type.getId())).toList()));
                })
                .toList();

        List<AnalyticsResponse.MonthStat> monthly = IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new AnalyticsResponse.MonthStat(month, requests.stream()
                        .filter(consuming.and(r -> r.getStartDate().getMonthValue() == month))
                        .mapToLong(LeaveRequest::getDays).sum()))
                .toList();

        BigDecimal threshold = policyService.teamAbsenceThresholdPercent();
        List<AnalyticsResponse.TeamStat> teams = teamRepository.findAllByOrderByName().stream()
                .map(team -> teamStat(team, today, threshold, requests, consuming))
                .toList();

        return new AnalyticsResponse(year, requests.size(), byStatus, approvedDays, utilization(balances), byType,
                teams, monthly);
    }

    private AnalyticsResponse.TeamStat teamStat(Team team, LocalDate today, BigDecimal threshold,
                                                List<LeaveRequest> yearRequests, Predicate<LeaveRequest> consuming) {
        int size = (int) employeeRepository.countByTeamId(team.getId());
        LocalDate horizon = today.plusDays(LOOKAHEAD_DAYS);
        List<LeaveRequest> upcoming = leaveRequestRepository.findTeamLeavesOverlapping(team.getId(), today, horizon,
                LeaveStatus.ACTIVE);
        List<DayAbsence> days = conflictDetectionService.dailyAbsence(size,
                dayCalculationService.workingDays(today, horizon), upcoming, threshold, null);

        int onLeaveToday = (int) upcoming.stream()
                .filter(r -> LeaveStatus.CONSUMING.contains(r.getStatus()) && r.overlaps(today, today))
                .map(r -> r.getEmployee().getId())
                .distinct()
                .count();
        DayAbsence peak = days.stream().max(Comparator.comparing(DayAbsence::absencePercent)).orElse(null);
        long approvedDays = yearRequests.stream()
                .filter(consuming.and(r -> r.getEmployee().getTeam() != null
                        && r.getEmployee().getTeam().getId().equals(team.getId())))
                .mapToLong(LeaveRequest::getDays)
                .sum();

        return new AnalyticsResponse.TeamStat(team.getId(), team.getName(), size, onLeaveToday,
                ConflictDetectionService.absencePercent(onLeaveToday, size),
                peak != null ? peak.absencePercent() : BigDecimal.ZERO,
                days.stream().anyMatch(DayAbsence::exceedsThreshold),
                approvedDays);
    }

    private static AnalyticsResponse.Utilization utilization(List<LeaveBalance> balances) {
        BigDecimal allocated = sum(balances, LeaveBalance::getAllocated);
        BigDecimal used = sum(balances, LeaveBalance::getUsed);
        BigDecimal pending = sum(balances, LeaveBalance::getPending);
        BigDecimal percent = allocated.signum() == 0
                ? BigDecimal.ZERO
                : used.multiply(BigDecimal.valueOf(100)).divide(allocated, 1, RoundingMode.HALF_UP);
        return new AnalyticsResponse.Utilization(allocated, used, pending, percent);
    }

    private static BigDecimal sum(List<LeaveBalance> balances, Function<LeaveBalance, BigDecimal> field) {
        return balances.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
