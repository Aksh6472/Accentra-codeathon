package com.accentra.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "leave_policies")
@Getter
@Setter
@NoArgsConstructor
public class LeavePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false, unique = true)
    private LeaveType leaveType;

    @Column(name = "annual_entitlement", nullable = false, precision = 5, scale = 1)
    private BigDecimal annualEntitlement;

    /** When true, employees joining mid-year receive a proportional entitlement. */
    @Column(nullable = false)
    private boolean prorated = true;

    /** When true, Saturdays and Sundays inside a leave range are charged as leave days. */
    @Column(name = "count_weekends", nullable = false)
    private boolean countWeekends;

    /** When true, public holidays inside a leave range are charged as leave days. */
    @Column(name = "count_holidays", nullable = false)
    private boolean countHolidays;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
