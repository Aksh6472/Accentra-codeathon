package com.accentra.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Organisation-wide workflow configuration. Exactly one row (id = 1) exists. */
@Entity
@Table(name = "workflow_settings")
@Getter
@Setter
@NoArgsConstructor
public class WorkflowSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "escalation_timeout_minutes", nullable = false)
    private Integer escalationTimeoutMinutes;

    @Column(name = "team_absence_threshold_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal teamAbsenceThresholdPercent;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
