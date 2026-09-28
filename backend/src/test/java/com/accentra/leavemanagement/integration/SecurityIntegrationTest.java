package com.accentra.leavemanagement.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends IntegrationTestSupport {

    private String employee;
    private String manager;
    private String otherManager;
    private String hr;

    @BeforeEach
    void signIn() throws Exception {
        employee = login("employee1@demo.com");
        manager = login("manager@demo.com");
        otherManager = login("manager2@demo.com");
        hr = login("hr@demo.com");
    }

    @Test
    void invalidCredentialsAreRejectedWithoutRevealingWhichPartWasWrong() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "employee1@demo.com", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@demo.com", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void loginResponseNeverExposesThePasswordHash() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "employee1@demo.com", "password", PASSWORD))))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("password").doesNotContain("$2a$");
    }

    @Test
    void protectedEndpointsRequireAValidToken() throws Exception {
        mvc.perform(get("/api/leaves/my")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        getAs("not-a-jwt", "/api/employees/me").andExpect(status().isUnauthorized());
    }

    @Test
    void employeeAccess() throws Exception {
        getAs(employee, "/api/employees/me").andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("employee1@demo.com"));
        getAs(employee, "/api/employees/me/balance").andExpect(status().isOk());
        getAs(employee, "/api/leaves/my").andExpect(status().isOk());
        getAs(employee, "/api/policies").andExpect(status().isOk());

        getAs(employee, "/api/manager/leaves/pending").andExpect(status().isForbidden());
        getAs(employee, "/api/hr/leaves/pending").andExpect(status().isForbidden());
        getAs(employee, "/api/analytics").andExpect(status().isForbidden());
        getAs(employee, "/api/teams").andExpect(status().isForbidden());
        getAs(employee, "/api/settings").andExpect(status().isForbidden());
        putAs(employee, "/api/policies/{id}", Map.of("name", "Hack", "annualEntitlement", 99), 1)
                .andExpect(status().isForbidden());
        postAs(employee, "/api/holidays", Map.of("name", "Free day", "date", "2026-12-01"))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCannotSeeOrApproveSomeoneElsesLeave() throws Exception {
        long colleagueLeave = firstLeaveOf("employee3@demo.com");
        getAs(employee, "/api/leaves/{id}", colleagueLeave).andExpect(status().isForbidden());
        postAs(employee, "/api/manager/leaves/{id}/approve", null, colleagueLeave).andExpect(status().isForbidden());
        postAs(employee, "/api/leaves/{id}/cancel", null, colleagueLeave).andExpect(status().isForbidden());
    }

    @Test
    void managerAccess() throws Exception {
        getAs(manager, "/api/manager/leaves/pending").andExpect(status().isOk());
        getAs(manager, "/api/teams").andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Engineering"));
        long teamLeave = firstLeaveOf("employee3@demo.com");
        getAs(manager, "/api/leaves/{id}", teamLeave).andExpect(status().isOk())
                .andExpect(jsonPath("$.teamConflicts").exists());

        getAs(manager, "/api/hr/leaves/pending").andExpect(status().isForbidden());
        getAs(manager, "/api/analytics").andExpect(status().isForbidden());
        postAs(manager, "/api/hr/leaves/{id}/approve", null, teamLeave).andExpect(status().isForbidden());
        postAs(manager, "/api/leaves", Map.of("leaveTypeId", 1, "startDate", "2026-12-01", "endDate", "2026-12-01",
                "reason", "x")).andExpect(status().isForbidden());
    }

    @Test
    void managersOnlySeeTheirOwnTeams() throws Exception {
        long engineeringLeave = firstLeaveOf("employee3@demo.com");
        getAs(otherManager, "/api/leaves/{id}", engineeringLeave).andExpect(status().isForbidden());

        long engineeringTeam = read(getAs(manager, "/api/teams")).get(0).get("id").asLong();
        getAs(otherManager, "/api/teams/{id}/calendar?from=2026-10-01&to=2026-10-31", engineeringTeam)
                .andExpect(status().isForbidden());
        getAs(manager, "/api/teams/{id}/calendar?from=2026-10-01&to=2026-10-31", engineeringTeam)
                .andExpect(status().isOk());
    }

    @Test
    void hrAccess() throws Exception {
        getAs(hr, "/api/hr/leaves/pending").andExpect(status().isOk());
        getAs(hr, "/api/hr/leaves/escalated").andExpect(status().isOk());
        getAs(hr, "/api/hr/audit").andExpect(status().isOk());
        getAs(hr, "/api/analytics").andExpect(status().isOk()).andExpect(jsonPath("$.totalRequests").isNumber());
        getAs(hr, "/api/settings").andExpect(status().isOk());
        getAs(hr, "/api/teams").andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));

        getAs(hr, "/api/manager/leaves/pending").andExpect(status().isForbidden());
        getAs(hr, "/api/leaves/my").andExpect(status().isForbidden());
    }

    @Test
    void validationErrorsAreReportedPerField() throws Exception {
        postAs(employee, "/api/leaves", Map.of("reason", "")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
        putAs(hr, "/api/settings", Map.of("escalationTimeoutMinutes", 0, "teamAbsenceThresholdPercent", 150))
                .andExpect(status().isBadRequest());
    }

    private long firstLeaveOf(String email) throws Exception {
        for (JsonNode leave : read(getAs(hr, "/api/hr/leaves"))) {
            if (leave.get("employeeCode").asText().equals(codeFor(email))) {
                return leave.get("id").asLong();
            }
        }
        throw new IllegalStateException("No seeded leave for " + email);
    }

    private static String codeFor(String email) {
        String number = email.replaceAll("\\D", "");
        return "EMP" + "0".repeat(3 - number.length()) + number;
    }
}
