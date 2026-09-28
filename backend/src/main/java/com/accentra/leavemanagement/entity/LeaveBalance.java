package com.accentra.leavemanagement.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "leave_balances",
        uniqueConstraints = @UniqueConstraint(name = "uk_leave_balances_employee_type_year",
                columnNames = {"employee_id", "leave_type_id", "balance_year"}))
@Getter
@Setter
@NoArgsConstructor
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "balance_year", nullable = false)
    private Integer year;

    /** Full-year entitlement from the policy at allocation time. */
    @Column(name = "annual_entitlement", nullable = false, precision = 5, scale = 1)
    private BigDecimal annualEntitlement;

    /** Entitlement actually granted for this year after proration. */
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal allocated;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal used = BigDecimal.ZERO;

    /** Days held by requests that are still awaiting approval. */
    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal pending = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private Long version;

    public BigDecimal getRemaining() {
        return allocated.subtract(used).subtract(pending);
    }
}
