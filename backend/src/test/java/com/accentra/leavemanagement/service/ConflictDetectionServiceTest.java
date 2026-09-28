package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.TestData;
import com.accentra.leavemanagement.dto.TeamConflictAnalysis;
import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.entity.LeaveType;
import com.accentra.leavemanagement.entity.Team;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import com.accentra.leavemanagement.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConflictDetectionServiceTest {

    private static final LocalDate MON = LocalDate.of(2026, 10, 12);
    private static final BigDecimal THRESHOLD = new BigDecimal("30");

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private LeaveDayCalculationService dayCalculationService;
    @Mock
    private PolicyService policyService;

    private ConflictDetectionService service;
    private Team team;
    private final List<Employee> members = new ArrayList<>();
    private final LeaveType casual = TestData.leaveType(1, "CASUAL");
    private final List<LocalDate> week = List.of(MON, MON.plusDays(1), MON.plusDays(2), MON.plusDays(3), MON.plusDays(4));

    @BeforeEach
    void setUp() {
        service = new ConflictDetectionService(leaveRequestRepository, employeeRepository, dayCalculationService,
                policyService);
        team = TestData.team(7, "Engineering");
        Employee manager = TestData.employee(100, "Manager", null, null);
        for (int i = 1; i <= 10; i++) {
            members.add(TestData.employee(i, "Member " + i, team, manager));
        }
    }

    private LeaveRequest leave(long id, Employee who, LocalDate start, LocalDate end, LeaveStatus status) {
        return TestData.request(id, who, casual, start, end, 1, status);
    }

    private TeamConflictAnalysis analyze(Employee applicant, LocalDate start, LocalDate end, List<LeaveRequest> others) {
        return service.buildAnalysis(team, members.size(), applicant, week.stream()
                .filter(d -> !d.isBefore(start) && !d.isAfter(end)).toList(), others, THRESHOLD);
    }

    @Test
    void noConflictWhenNobodyElseIsAway() {
        TeamConflictAnalysis result = analyze(members.get(0), MON, MON.plusDays(1), List.of());

        assertThat(result.hasConflict()).isFalse();
        assertThat(result.teamLeaveWarning()).isFalse();
        assertThat(result.peakUnavailable()).isEqualTo(1);
        assertThat(result.peakAbsencePercent()).isEqualByComparingTo("10.0");
        assertThat(result.affectedDates()).isEmpty();
        assertThat(result.warningMessage()).isNull();
    }

    @Test
    void partialOverlapIsReportedOnlyOnTheSharedDays() {
        // Applicant Mon–Wed, colleague Wed–Fri → only Wednesday overlaps.
        LeaveRequest colleague = leave(2, members.get(1), MON.plusDays(2), MON.plusDays(4), LeaveStatus.APPROVED);

        TeamConflictAnalysis result = analyze(members.get(0), MON, MON.plusDays(2), List.of(colleague));

        assertThat(result.hasConflict()).isTrue();
        assertThat(result.teamLeaveWarning()).isFalse();
        assertThat(result.affectedDates()).singleElement().satisfies(day -> {
            assertThat(day.date()).isEqualTo(MON.plusDays(2));
            assertThat(day.unavailableCount()).isEqualTo(2);
            assertThat(day.absencePercent()).isEqualByComparingTo("20.0");
            assertThat(day.employeeNames()).containsExactly("Member 2", "Member 1");
        });
        assertThat(result.overlappingLeaves()).extracting(TeamConflictAnalysis.OverlappingLeave::employeeName)
                .containsExactly("Member 2");
    }

    @Test
    void multipleOverlappingEmployeesAreCountedOncePerDay() {
        List<LeaveRequest> others = List.of(
                leave(2, members.get(1), MON, MON.plusDays(4), LeaveStatus.APPROVED),
                leave(3, members.get(2), MON.plusDays(1), MON.plusDays(1), LeaveStatus.PENDING_MANAGER),
                leave(4, members.get(3), MON.plusDays(1), MON.plusDays(3), LeaveStatus.PENDING_HR));

        TeamConflictAnalysis result = analyze(members.get(0), MON, MON.plusDays(1), others);

        assertThat(result.peakUnavailable()).isEqualTo(4);
        assertThat(result.peakDate()).isEqualTo(MON.plusDays(1));
        assertThat(result.peakAbsencePercent()).isEqualByComparingTo("40.0");
        assertThat(result.overlappingLeaves()).hasSize(3);
    }

    @Test
    void excessiveAbsenceRaisesWarningButNeverRejects() {
        // 4 of 10 away on Monday = 40% > 30% threshold (the spec example).
        List<LeaveRequest> others = List.of(
                leave(2, members.get(1), MON, MON, LeaveStatus.APPROVED),
                leave(3, members.get(2), MON, MON, LeaveStatus.APPROVED),
                leave(4, members.get(3), MON, MON, LeaveStatus.APPROVED));

        TeamConflictAnalysis result = analyze(members.get(0), MON, MON, others);

        assertThat(result.teamLeaveWarning()).isTrue();
        assertThat(result.teamSize()).isEqualTo(10);
        assertThat(result.peakUnavailable()).isEqualTo(4);
        assertThat(result.warningMessage())
                .startsWith("High team absence: 40% of the team (4 of 10) is unavailable on 12 Oct 2026");
        assertThat(result.affectedDates()).singleElement()
                .satisfies(day -> assertThat(day.exceedsThreshold()).isTrue());
    }

    @Test
    void absenceExactlyAtThresholdIsNotExcessive() {
        List<LeaveRequest> others = List.of(
                leave(2, members.get(1), MON, MON, LeaveStatus.APPROVED),
                leave(3, members.get(2), MON, MON, LeaveStatus.APPROVED));

        TeamConflictAnalysis result = analyze(members.get(0), MON, MON, others);

        assertThat(result.peakAbsencePercent()).isEqualByComparingTo("30.0");
        assertThat(result.teamLeaveWarning()).isFalse();
        assertThat(result.hasConflict()).isTrue();
    }

    @Test
    void analyzeExcludesTheApplicantsOwnRequestsAndUsesConfiguredThreshold() {
        Employee applicant = members.get(0);
        when(policyService.teamAbsenceThresholdPercent()).thenReturn(THRESHOLD);
        when(employeeRepository.countByTeamId(7L)).thenReturn(10L);
        when(dayCalculationService.workingDays(MON, MON)).thenReturn(List.of(MON));
        when(leaveRequestRepository.findTeamLeavesOverlapping(eq(7L), eq(MON), eq(MON), any()))
                .thenReturn(List.of(leave(1, applicant, MON, MON, LeaveStatus.PENDING_MANAGER),
                        leave(2, members.get(1), MON, MON, LeaveStatus.APPROVED)));

        TeamConflictAnalysis result = service.analyze(applicant, MON, MON);

        assertThat(result.peakUnavailable()).isEqualTo(2);
        assertThat(result.overlappingLeaves()).hasSize(1);
    }

    @Test
    void employeeWithoutTeamHasNoConflicts() {
        when(policyService.teamAbsenceThresholdPercent()).thenReturn(THRESHOLD);
        Employee loner = TestData.employee(50, "Solo", null, members.get(0));

        TeamConflictAnalysis result = service.analyze(loner, MON, MON);

        assertThat(result.teamId()).isNull();
        assertThat(result.teamLeaveWarning()).isFalse();
    }

    @Test
    void employeeViewHidesColleagueNames() {
        LeaveRequest colleague = leave(2, members.get(1), MON, MON, LeaveStatus.APPROVED);
        TeamConflictAnalysis result = analyze(members.get(0), MON, MON, List.of(colleague)).withoutPersonalData();

        assertThat(result.overlappingLeaves()).isEmpty();
        assertThat(result.affectedDates()).allSatisfy(day -> assertThat(day.employeeNames()).isEmpty());
        assertThat(result.hasConflict()).isTrue();
    }
}
