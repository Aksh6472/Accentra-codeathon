package com.accentra.leavemanagement.integration;

import com.accentra.leavemanagement.entity.LeaveRequest;
import com.accentra.leavemanagement.repository.LeaveRequestRepository;
import com.accentra.leavemanagement.service.EscalationService;
import com.accentra.leavemanagement.service.LeaveProrationService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeaveWorkflowIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;
    @Autowired
    private EscalationService escalationService;
    @Autowired
    private LeaveProrationService prorationService;

    @Test
    void employeeToManagerToHrApprovalUpdatesBalanceHistoryAndNotifications() throws Exception {
        String employee = login("employee11@demo.com");
        String manager = login("manager2@demo.com");
        String hr = login("hr@demo.com");
        long casual = leaveTypeId(employee, "CASUAL");
        LocalDate start = quietMonday(35);

        BigDecimal usedBefore = balance(employee, "CASUAL").get("used").decimalValue();

        JsonNode created = read(postAs(employee, "/api/leaves", Map.of("leaveTypeId", casual,
                "startDate", start.toString(), "endDate", start.plusDays(1).toString(), "reason", "Family visit"))
                .andExpect(status().isCreated()));
        long id = created.at("/request/id").asLong();
        assertThat(created.at("/request/status").asText()).isEqualTo("PENDING_MANAGER");
        assertThat(created.at("/request/days").asInt()).isEqualTo(2);
        assertThat(created.at("/balance/pending").decimalValue()).isEqualByComparingTo("2");

        postAs(hr, "/api/hr/leaves/{id}/approve", null, id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));

        postAs(manager, "/api/manager/leaves/{id}/approve", Map.of("comment", "Enjoy"), id)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_HR"));
        postAs(manager, "/api/manager/leaves/{id}/approve", null, id).andExpect(status().isConflict());

        postAs(hr, "/api/hr/leaves/{id}/approve", null, id)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        // Retrying the final approval must not deduct the balance twice.
        postAs(hr, "/api/hr/leaves/{id}/approve", null, id).andExpect(status().isConflict());

        JsonNode balance = balance(employee, "CASUAL");
        assertThat(balance.get("used").decimalValue()).isEqualByComparingTo(usedBefore.add(BigDecimal.valueOf(2)));
        assertThat(balance.get("pending").decimalValue()).isEqualByComparingTo("0");

        List<String> actions = new ArrayList<>();
        read(getAs(employee, "/api/leaves/{id}", id)).get("history").forEach(h -> actions.add(h.get("action").asText()));
        assertThat(actions).containsExactly("CREATED", "BALANCE_UPDATED", "MANAGER_APPROVED", "HR_APPROVED",
                "BALANCE_UPDATED");

        List<String> titles = new ArrayList<>();
        read(getAs(employee, "/api/notifications")).get("notifications").forEach(n -> titles.add(n.get("title").asText()));
        assertThat(titles).contains("Leave request submitted", "Manager approved your leave", "Leave approved");

        // Cancellation of upcoming approved leave restores the balance once approved.
        postAs(employee, "/api/leaves/{id}/cancel", Map.of("comment", "Plans changed"), id)
                .andExpect(jsonPath("$.status").value("CANCEL_REQUESTED"));
        postAs(manager, "/api/manager/leaves/{id}/cancellation/approve", null, id)
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(balance(employee, "CASUAL").get("used").decimalValue()).isEqualByComparingTo(usedBefore);
    }

    @Test
    void rejectionRequiresCommentAndReleasesPendingDays() throws Exception {
        String employee = login("employee10@demo.com");
        String manager = login("manager2@demo.com");
        long sick = leaveTypeId(employee, "SICK");
        LocalDate start = quietMonday(49);
        BigDecimal remainingBefore = balance(employee, "SICK").get("remaining").decimalValue();

        long id = read(postAs(employee, "/api/leaves", Map.of("leaveTypeId", sick, "startDate", start.toString(),
                "endDate", start.toString(), "reason", "Check-up"))).at("/request/id").asLong();

        postAs(manager, "/api/manager/leaves/{id}/reject", null, id).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("COMMENT_REQUIRED"));
        postAs(manager, "/api/manager/leaves/{id}/reject", Map.of("comment", "Team offsite that day"), id)
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(balance(employee, "SICK").get("remaining").decimalValue()).isEqualByComparingTo(remainingBefore);
    }

    @Test
    void teamConflictProducesWarningButRequestStaysPendingForTheManager() throws Exception {
        String employee = login("employee1@demo.com");
        String manager = login("manager@demo.com");
        // Seed data: two of seven engineers are already away Tue/Wed of the week after next.
        LocalDate window = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);

        JsonNode created = read(postAs(employee, "/api/leaves", Map.of("leaveTypeId", leaveTypeId(employee, "CASUAL"),
                "startDate", window.plusDays(1).toString(), "endDate", window.plusDays(2).toString(),
                "reason", "Conference")).andExpect(status().isCreated()));

        assertThat(created.at("/request/status").asText()).isEqualTo("PENDING_MANAGER");
        assertThat(created.at("/request/teamLeaveWarning").asBoolean()).isTrue();
        assertThat(created.at("/teamConflicts/peakAbsencePercent").decimalValue()).isEqualByComparingTo("42.9");
        assertThat(created.at("/teamConflicts/overlappingLeaves").size()).isZero(); // hidden from employees

        long id = created.at("/request/id").asLong();
        JsonNode review = read(getAs(manager, "/api/leaves/{id}", id));
        assertThat(review.at("/teamConflicts/teamLeaveWarning").asBoolean()).isTrue();
        assertThat(review.at("/teamConflicts/overlappingLeaves").size()).isEqualTo(2);
        assertThat(review.at("/request/availableActions").toString()).contains("MANAGER_APPROVE", "MANAGER_REJECT");

        List<String> managerTitles = new ArrayList<>();
        read(getAs(manager, "/api/notifications")).get("notifications")
                .forEach(n -> managerTitles.add(n.get("title").asText()));
        assertThat(managerTitles).contains("High team absence warning", "New leave request");

        // The manager can still approve despite the warning.
        postAs(manager, "/api/manager/leaves/{id}/approve", null, id).andExpect(jsonPath("$.status").value("PENDING_HR"));
    }

    @Test
    void overdueManagerApprovalIsEscalatedToHrExactlyOnce() throws Exception {
        String employee = login("employee2@demo.com");
        String hr = login("hr@demo.com");
        LocalDate start = quietMonday(63);

        long id = read(postAs(employee, "/api/leaves", Map.of("leaveTypeId", leaveTypeId(employee, "EARNED"),
                "startDate", start.toString(), "endDate", start.toString(), "reason", "Errand"))).at("/request/id").asLong();

        LeaveRequest request = leaveRequestRepository.findById(id).orElseThrow();
        request.setCreatedAt(request.getCreatedAt().minus(Duration.ofDays(2)));
        leaveRequestRepository.save(request);

        assertThat(escalationService.escalateOverdueRequests()).isGreaterThanOrEqualTo(1);
        assertThat(escalationService.escalateOverdueRequests()).isZero();

        JsonNode detail = read(getAs(hr, "/api/leaves/{id}", id));
        assertThat(detail.at("/request/status").asText()).isEqualTo("ESCALATED");
        List<String> actions = new ArrayList<>();
        detail.get("history").forEach(h -> actions.add(h.get("action").asText()));
        assertThat(actions.stream().filter("ESCALATED"::equals).count()).isEqualTo(1);

        List<Long> escalatedIds = new ArrayList<>();
        read(getAs(hr, "/api/hr/leaves/escalated")).forEach(r -> escalatedIds.add(r.get("id").asLong()));
        assertThat(escalatedIds).contains(id);

        List<String> hrTitles = new ArrayList<>();
        read(getAs(hr, "/api/notifications")).get("notifications").forEach(n -> hrTitles.add(n.get("title").asText()));
        assertThat(hrTitles).contains("Leave request escalated");

        postAs(hr, "/api/hr/leaves/{id}/approve", null, id).andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void midYearJoinerHasProratedBalance() throws Exception {
        String newJoiner = login("employee2@demo.com");
        LocalDate joined = LocalDate.parse(read(getAs(newJoiner, "/api/employees/me")).get("joiningDate").asText());
        JsonNode earned = balance(newJoiner, "EARNED");

        BigDecimal expected = prorationService.proratedEntitlement(new BigDecimal("24"), true, joined,
                LocalDate.now().getYear());
        assertThat(earned.get("annualEntitlement").decimalValue()).isEqualByComparingTo("24");
        assertThat(earned.get("allocated").decimalValue()).isEqualByComparingTo(expected);
        assertThat(expected).isLessThan(new BigDecimal("24"));
    }

    @Test
    void overlappingOwnLeaveAndInsufficientBalanceAreRejected() throws Exception {
        String employee = login("employee9@demo.com");
        long casual = leaveTypeId(employee, "CASUAL");
        LocalDate start = quietMonday(77);
        Map<String, Object> body = Map.of("leaveTypeId", casual, "startDate", start.toString(),
                "endDate", start.toString(), "reason", "Personal");

        postAs(employee, "/api/leaves", body).andExpect(status().isCreated());
        postAs(employee, "/api/leaves", body).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OVERLAPPING_LEAVE"));

        // Three weeks (≥ 14 working days) against a 12-day casual allocation, kept inside one calendar year.
        LocalDate longStart = start.plusWeeks(1);
        if (longStart.plusDays(20).getYear() != longStart.getYear()) {
            longStart = LocalDate.of(longStart.getYear() + 1, 1, 5).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        }
        postAs(employee, "/api/leaves", Map.of("leaveTypeId", casual, "startDate", longStart.toString(),
                "endDate", longStart.plusDays(20).toString(), "reason", "Long trip"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));

        postAs(employee, "/api/leaves", Map.of("leaveTypeId", casual, "startDate", "2020-01-06",
                "endDate", "2020-01-06", "reason", "Past")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    private JsonNode balance(String token, String code) throws Exception {
        for (JsonNode b : read(getAs(token, "/api/employees/me/balance"))) {
            if (b.get("leaveTypeCode").asText().equals(code)) {
                return b;
            }
        }
        throw new IllegalStateException("No balance for " + code);
    }

    /** A Monday at least {@code days} ahead whose Mon–Tue fall in the same year as the Monday. */
    private static LocalDate quietMonday(int days) {
        LocalDate monday = LocalDate.now().plusDays(days).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        while (monday.plusDays(1).getYear() != monday.getYear() || isFixedHoliday(monday) || isFixedHoliday(monday.plusDays(1))) {
            monday = monday.plusWeeks(1);
        }
        return monday;
    }

    private static boolean isFixedHoliday(LocalDate d) {
        return List.of("01-01", "01-26", "05-01", "08-15", "10-02", "12-25")
                .contains(d.toString().substring(5));
    }
}
