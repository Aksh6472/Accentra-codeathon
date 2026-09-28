package com.accentra.leavemanagement.config;

import com.accentra.leavemanagement.dto.TeamConflictAnalysis;
import com.accentra.leavemanagement.entity.AuditLog;
import com.accentra.leavemanagement.entity.Employee;
import com.accentra.leavemanagement.entity.Holiday;
import com.accentra.leavemanagement.entity.LeaveBalance;
import com.accentra.leavemanagement.entity.LeavePolicy;
import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.entity.LeaveType;
import com.accentra.leavemanagement.entity.Notification;
import com.accentra.leavemanagement.entity.Team;
import com.accentra.leavemanagement.entity.User;
import com.accentra.leavemanagement.enums.ActorRole;
import com.accentra.leavemanagement.enums.AuditAction;
import com.accentra.leavemanagement.enums.LeaveStatus;
import com.accentra.leavemanagement.enums.NotificationType;
import com.accentra.leavemanagement.enums.Role;
import com.accentra.leavemanagement.repository.AuditLogRepository;
import com.accentra.leavemanagement.repository.EmployeeRepository;
import com.accentra.leavemanagement.repository.HolidayRepository;
import com.accentra.leavemanagement.repository.LeavePolicyRepository;
import com.accentra.leavemanagement.repository.LeaveRequestRepository;
import com.accentra.leavemanagement.repository.LeaveTypeRepository;
import com.accentra.leavemanagement.repository.NotificationRepository;
import com.accentra.leavemanagement.repository.TeamRepository;
import com.accentra.leavemanagement.repository.UserRepository;
import com.accentra.leavemanagement.service.ConflictDetectionService;
import com.accentra.leavemanagement.service.LeaveBalanceService;
import com.accentra.leavemanagement.service.LeaveDayCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Demo data for local development and demonstrations. Runs once, on an empty database, when
 * {@code app.seed.enabled=true}. All dates are relative to "today" so the demo always looks current.
 * Seeded history is written directly (it did not happen through the API) but balances, audit
 * entries and notifications are kept consistent with each request's status.
 */
@Slf4j
@Component
@Order(2)
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final AppProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final TeamRepository teamRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeavePolicyRepository policyRepository;
    private final HolidayRepository holidayRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository;
    private final LeaveBalanceService balanceService;
    private final LeaveDayCalculationService dayCalculationService;
    private final ConflictDetectionService conflictDetectionService;

    private String passwordHash;
    private Instant now;
    private LocalDate today;
    private Employee hrEmployee;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("Demo data already present; skipping seeding");
            return;
        }
        now = clock.instant();
        today = LocalDate.now(clock);
        passwordHash = passwordEncoder.encode(properties.seed().demoPassword());

        Map<String, LeaveType> types = seedPolicies();
        seedHolidays(today.getYear());
        seedHolidays(today.getYear() + 1);

        hrEmployee = employee("hr@demo.com", Role.HR, "HR001", "Hannah Reed", "HR Business Partner",
                LocalDate.of(2019, 4, 1), null, null);
        Employee engManager = employee("manager@demo.com", Role.MANAGER, "MGR001", "Marcus Chen",
                "Engineering Manager", LocalDate.of(2020, 1, 6), null, null);
        Employee designManager = employee("manager2@demo.com", Role.MANAGER, "MGR002", "Sofia Alvarez",
                "Head of Design", LocalDate.of(2021, 2, 1), null, null);

        Team engineering = team("Engineering", "Platform and product engineering", engManager);
        Team design = team("Design", "Product design and research", designManager);

        Employee aarav = employee("employee1@demo.com", Role.EMPLOYEE, "EMP001", "Aarav Patel",
                "Senior Software Engineer", LocalDate.of(2022, 3, 14), engineering, engManager);
        employee("employee2@demo.com", Role.EMPLOYEE, "EMP002", "Priya Nair",
                "Software Engineer", midYearJoiningDate(), engineering, engManager);
        Employee rahul = employee("employee3@demo.com", Role.EMPLOYEE, "EMP003", "Rahul Verma",
                "QA Engineer", LocalDate.of(2021, 8, 2), engineering, engManager);
        Employee emily = employee("employee4@demo.com", Role.EMPLOYEE, "EMP004", "Emily Johnson",
                "Frontend Engineer", LocalDate.of(2023, 1, 9), engineering, engManager);
        Employee daniel = employee("employee5@demo.com", Role.EMPLOYEE, "EMP005", "Daniel Kim",
                "Backend Engineer", LocalDate.of(2020, 11, 16), engineering, engManager);
        Employee meera = employee("employee6@demo.com", Role.EMPLOYEE, "EMP006", "Meera Iyer",
                "DevOps Engineer", LocalDate.of(2022, 6, 1), engineering, engManager);
        Employee lucas = employee("employee7@demo.com", Role.EMPLOYEE, "EMP007", "Lucas Martin",
                "Software Engineer", LocalDate.of(2024, 2, 12), engineering, engManager);

        Employee ananya = employee("employee8@demo.com", Role.EMPLOYEE, "EMP008", "Ananya Rao",
                "Product Designer", LocalDate.of(2022, 9, 5), design, designManager);
        Employee oliver = employee("employee9@demo.com", Role.EMPLOYEE, "EMP009", "Oliver Brown",
                "UX Researcher", LocalDate.of(2023, 4, 17), design, designManager);
        Employee zara = employee("employee10@demo.com", Role.EMPLOYEE, "EMP010", "Zara Khan",
                "UI Designer", LocalDate.of(2021, 10, 11), design, designManager);
        employee("employee11@demo.com", Role.EMPLOYEE, "EMP011", "Noah Wilson",
                "Visual Designer", LocalDate.of(2024, 7, 1), design, designManager);

        // Conflict window: the week after next. Two engineers are already away mid-week, so a third
        // request overlapping it pushes Engineering (7 people) past the 30% threshold.
        LocalDate window = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
        LeaveType casual = types.get("CASUAL");
        LeaveType sick = types.get("SICK");
        LeaveType earned = types.get("EARNED");

        request(rahul, earned, window, window.plusDays(2), LeaveStatus.APPROVED, hoursAgo(120),
                "Family function out of town");
        request(emily, casual, window.plusDays(1), window.plusDays(2), LeaveStatus.APPROVED, hoursAgo(96),
                "Moving to a new apartment");
        request(daniel, casual, window.plusDays(3), window.plusDays(4), LeaveStatus.PENDING_MANAGER, hoursAgo(2),
                "Attending a friend's wedding");
        request(meera, sick, today.plusDays(3), today.plusDays(3), LeaveStatus.PENDING_HR, hoursAgo(26),
                "Scheduled minor medical procedure");
        request(lucas, earned, window.plusWeeks(2), window.plusWeeks(2).plusDays(4), LeaveStatus.ESCALATED,
                hoursAgo(72), "Annual vacation");

        LocalDate pastStart = today.minusMonths(2).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        if (pastStart.getYear() == today.getYear()) {
            request(aarav, earned, pastStart, pastStart.plusDays(4), LeaveStatus.APPROVED,
                    Instant.from(pastStart.minusDays(20).atStartOfDay(clock.getZone())), "Summer holiday");
            request(aarav, casual, pastStart.plusWeeks(3), pastStart.plusWeeks(3), LeaveStatus.REJECTED,
                    Instant.from(pastStart.plusWeeks(2).atStartOfDay(clock.getZone())), "Personal errand");
        }

        request(ananya, casual, today.plusDays(4), today.plusDays(4), LeaveStatus.APPROVED, hoursAgo(50),
                "Personal appointment");
        // Submitted 30 hours ago and never reviewed: the escalation scheduler will escalate it shortly
        // after start-up with the default 24-hour timeout (demo scenario 2).
        request(oliver, casual, window.plusDays(7), window.plusDays(8), LeaveStatus.PENDING_MANAGER,
                hoursAgo(30), "Short break");
        request(zara, earned, window.plusWeeks(3), window.plusWeeks(3).plusDays(1), LeaveStatus.CANCEL_REQUESTED,
                hoursAgo(200), "Trip plans changed");

        log.info("Seeded demo data: {} users, {} teams, {} leave requests (demo password from app.seed.demo-password)",
                userRepository.count(), teamRepository.count(), leaveRequestRepository.count());
    }

    private Map<String, LeaveType> seedPolicies() {
        List<LeaveType> types = new ArrayList<>();
        types.add(policy("CASUAL", "Casual Leave", "Short personal time off", "12"));
        types.add(policy("SICK", "Sick Leave", "Illness or medical appointments", "10"));
        types.add(policy("EARNED", "Earned Leave", "Planned vacation, accrued monthly", "24"));
        return types.stream().collect(Collectors.toMap(LeaveType::getCode, Function.identity()));
    }

    private LeaveType policy(String code, String name, String description, String entitlement) {
        LeaveType type = new LeaveType();
        type.setCode(code);
        type.setName(name);
        type.setDescription(description);
        type.setActive(true);
        leaveTypeRepository.save(type);
        LeavePolicy policy = new LeavePolicy();
        policy.setLeaveType(type);
        policy.setAnnualEntitlement(new BigDecimal(entitlement));
        policy.setProrated(true);
        policy.setCountWeekends(false);
        policy.setCountHolidays(false);
        policy.setUpdatedAt(now);
        policyRepository.save(policy);
        return type;
    }

    private void seedHolidays(int year) {
        holidayRepository.save(new Holiday("New Year's Day", LocalDate.of(year, 1, 1)));
        holidayRepository.save(new Holiday("Republic Day", LocalDate.of(year, 1, 26)));
        holidayRepository.save(new Holiday("Labour Day", LocalDate.of(year, 5, 1)));
        holidayRepository.save(new Holiday("Independence Day", LocalDate.of(year, 8, 15)));
        holidayRepository.save(new Holiday("Gandhi Jayanti", LocalDate.of(year, 10, 2)));
        holidayRepository.save(new Holiday("Christmas Day", LocalDate.of(year, 12, 25)));
    }

    /**
     * 1 July of the current year once that date has passed (24 days/year → 12 days), otherwise the first
     * of the current month, so the new joiner always has a visibly pro-rated balance.
     */
    private LocalDate midYearJoiningDate() {
        LocalDate july = LocalDate.of(today.getYear(), 7, 1);
        return today.isBefore(july) ? today.withDayOfMonth(1) : july;
    }

    private Employee employee(String email, Role role, String code, String name, String title, LocalDate joined,
                              Team team, Employee manager) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setEnabled(true);
        user.setCreatedAt(now);
        userRepository.save(user);

        Employee employee = new Employee();
        employee.setUser(user);
        employee.setEmployeeCode(code);
        employee.setFullName(name);
        employee.setJobTitle(title);
        employee.setJoiningDate(joined);
        employee.setTeam(team);
        employee.setManager(manager);
        return employeeRepository.save(employee);
    }

    private Team team(String name, String description, Employee manager) {
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        team.setManager(manager);
        return teamRepository.save(team);
    }

    private Instant hoursAgo(long hours) {
        return now.minus(Duration.ofHours(hours));
    }

    private void request(Employee employee, LeaveType type, LocalDate start, LocalDate end, LeaveStatus status,
                         Instant createdAt, String reason) {
        LeavePolicy policy = policyRepository.findByLeaveTypeId(type.getId()).orElseThrow();
        int days = dayCalculationService.calculate(start, end, policy).chargeableDays();
        // Relative dates can land entirely on a weekend/holiday; slide forward to the next working day.
        while (days == 0) {
            start = start.plusDays(1);
            end = end.plusDays(1);
            days = dayCalculationService.calculate(start, end, policy).chargeableDays();
        }
        TeamConflictAnalysis conflicts = conflictDetectionService.analyze(employee, start, end);
        Employee manager = employee.getManager();

        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employee);
        request.setLeaveType(type);
        request.setApprover(manager);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setDays(days);
        request.setReason(reason);
        request.setStatus(status);
        request.setTeamAbsencePercent(conflicts.peakAbsencePercent().setScale(2, RoundingMode.HALF_UP));
        request.setTeamLeaveWarning(conflicts.teamLeaveWarning());
        request.setHasTeamConflict(conflicts.hasConflict());
        request.setCreatedAt(createdAt);
        request.setUpdatedAt(createdAt.plus(Duration.ofHours(1)));
        leaveRequestRepository.save(request);

        LeaveBalance balance = balanceService.getOrCreate(employee, type, start.getYear());
        if (LeaveStatus.AWAITING_DECISION.contains(status)) {
            balance.setPending(balance.getPending().add(BigDecimal.valueOf(days)));
        } else if (LeaveStatus.CONSUMING.contains(status)) {
            balance.setUsed(balance.getUsed().add(BigDecimal.valueOf(days)));
        }

        Instant t = createdAt;
        audit(request, employee, ActorRole.EMPLOYEE, AuditAction.CREATED, null, LeaveStatus.PENDING_MANAGER, null, t);
        switch (status) {
            case PENDING_MANAGER -> notify(manager, NotificationType.NEW_LEAVE_REQUEST, "New leave request",
                    "%s requested %s (%d day(s)).".formatted(employee.getFullName(), type.getName(), days), request, t);
            case PENDING_HR -> {
                audit(request, manager, ActorRole.MANAGER, AuditAction.MANAGER_APPROVED, LeaveStatus.PENDING_MANAGER,
                        LeaveStatus.PENDING_HR, "Approved", t = t.plus(Duration.ofHours(1)));
                notify(hrEmployee, NotificationType.AWAITING_HR_APPROVAL, "Leave awaiting HR approval",
                        "%s's %s was approved by %s and needs HR approval."
                                .formatted(employee.getFullName(), type.getName(), manager.getFullName()), request, t);
            }
            case ESCALATED -> {
                t = t.plus(Duration.ofHours(24));
                audit(request, null, ActorRole.SYSTEM, AuditAction.ESCALATED, LeaveStatus.PENDING_MANAGER,
                        LeaveStatus.ESCALATED, "No manager decision within 24 hours; escalated to HR automatically.", t);
                request.setEscalatedAt(t);
                notify(hrEmployee, NotificationType.LEAVE_ESCALATED, "Leave request escalated",
                        "%s's %s received no manager decision in time and was escalated to HR."
                                .formatted(employee.getFullName(), type.getName()), request, t);
            }
            case REJECTED -> {
                audit(request, manager, ActorRole.MANAGER, AuditAction.MANAGER_REJECTED, LeaveStatus.PENDING_MANAGER,
                        LeaveStatus.REJECTED, "Release week - please pick another day", t = t.plus(Duration.ofHours(3)));
                notify(employee, NotificationType.LEAVE_REJECTED, "Leave rejected",
                        "Your %s request was rejected by %s.".formatted(type.getName(), manager.getFullName()),
                        request, t);
            }
            case APPROVED, CANCEL_REQUESTED -> {
                audit(request, manager, ActorRole.MANAGER, AuditAction.MANAGER_APPROVED, LeaveStatus.PENDING_MANAGER,
                        LeaveStatus.PENDING_HR, "Approved", t = t.plus(Duration.ofHours(2)));
                audit(request, hrEmployee, ActorRole.HR, AuditAction.HR_APPROVED, LeaveStatus.PENDING_HR,
                        LeaveStatus.APPROVED, null, t = t.plus(Duration.ofHours(3)));
                audit(request, null, ActorRole.SYSTEM, AuditAction.BALANCE_UPDATED, LeaveStatus.APPROVED,
                        LeaveStatus.APPROVED, "Moved %d day(s) from pending to used".formatted(days), t);
                notify(employee, NotificationType.HR_APPROVED, "Leave approved",
                        "Your %s request has been fully approved.".formatted(type.getName()), request, t);
                if (status == LeaveStatus.CANCEL_REQUESTED) {
                    audit(request, employee, ActorRole.EMPLOYEE, AuditAction.CANCELLATION_REQUESTED,
                            LeaveStatus.APPROVED, LeaveStatus.CANCEL_REQUESTED, "Trip plans changed",
                            t = t.plus(Duration.ofHours(20)));
                    notify(manager, NotificationType.CANCELLATION_REQUESTED, "Cancellation requested",
                            "%s asked to cancel their approved %s.".formatted(employee.getFullName(), type.getName()),
                            request, t);
                }
            }
            default -> {
            }
        }
        request.setUpdatedAt(t);
    }

    private void audit(LeaveRequest request, Employee actor, ActorRole role, AuditAction action, LeaveStatus from,
                       LeaveStatus to, String comment, Instant at) {
        AuditLog log = new AuditLog();
        log.setLeaveRequest(request);
        log.setActor(actor);
        log.setActorName(actor != null ? actor.getFullName() : "System");
        log.setActorRole(role);
        log.setAction(action);
        log.setPreviousStatus(from);
        log.setNewStatus(to);
        log.setComment(comment);
        log.setCreatedAt(at);
        auditLogRepository.save(log);
    }

    private void notify(Employee recipient, NotificationType type, String title, String message,
                        LeaveRequest request, Instant at) {
        Notification notification = new Notification();
        notification.setRecipient(recipient.getUser());
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLeaveRequest(request);
        notification.setRead(at.isBefore(now.minus(Duration.ofDays(3))));
        notification.setCreatedAt(at);
        notificationRepository.save(notification);
    }
}
