package com.accentra.leavemanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timeZone,
        Jwt jwt,
        Cors cors,
        Workflow workflow,
        Escalation escalation,
        Seed seed) {

    public record Jwt(String secret, long expirationMinutes) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    /** Initial values for the workflow_settings row; the database is authoritative afterwards. */
    public record Workflow(int defaultEscalationTimeoutMinutes, BigDecimal defaultTeamAbsenceThresholdPercent) {
    }

    public record Escalation(long checkIntervalMs, long initialDelayMs) {
    }

    public record Seed(boolean enabled, String demoPassword) {
    }
}
