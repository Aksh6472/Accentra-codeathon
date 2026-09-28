package com.accentra.leavemanagement.entity;

import com.accentra.leavemanagement.enums.LeaveStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    /** The reporting manager at submission time; the only manager allowed to act on this request. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "approver_id", nullable = false)
    private Employee approver;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Chargeable leave days (weekends/holidays excluded according to policy). */
    @Column(nullable = false)
    private Integer days;

    @Column(nullable = false, length = 500)
    private String reason;

    /** Changed only through LeaveStateMachineService. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LeaveStatus status;

    /** Peak team absence (including this request) measured at submission. */
    @Column(name = "team_absence_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal teamAbsencePercent = BigDecimal.ZERO;

    @Column(name = "team_leave_warning", nullable = false)
    private boolean teamLeaveWarning;

    @Column(name = "has_team_conflict", nullable = false)
    private boolean hasTeamConflict;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "escalated_at")
    private Instant escalatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public boolean overlaps(LocalDate from, LocalDate to) {
        return !startDate.isAfter(to) && !endDate.isBefore(from);
    }
}
