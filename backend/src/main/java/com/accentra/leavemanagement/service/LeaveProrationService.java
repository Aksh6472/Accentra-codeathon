package com.accentra.leavemanagement.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Pro-rates annual leave entitlement for employees who join part-way through a year.
 * <p>
 * Convention (monthly accrual, mid-month cut-off):
 * <ul>
 *   <li>Joined before the year starts: full entitlement.</li>
 *   <li>Joined after the year ends: zero.</li>
 *   <li>Joined during the year: the joining month counts as a full month if the joining day is
 *       on or before the 15th, otherwise accrual starts the following month.</li>
 *   <li>Entitlement = annual × eligibleMonths / 12, rounded half-up to the nearest 0.5 day.</li>
 * </ul>
 * Example: 24 days/year, joined 1 July → 6 eligible months → 12 days. Joined 20 July → 5 months → 10 days.
 */
@Service
public class LeaveProrationService {

    static final int MID_MONTH_CUTOFF_DAY = 15;
    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    public int eligibleMonths(LocalDate joiningDate, int year) {
        if (joiningDate.getYear() < year) {
            return 12;
        }
        if (joiningDate.getYear() > year) {
            return 0;
        }
        int firstAccruingMonth = joiningDate.getDayOfMonth() <= MID_MONTH_CUTOFF_DAY
                ? joiningDate.getMonthValue()
                : joiningDate.getMonthValue() + 1;
        return 12 - firstAccruingMonth + 1;
    }

    public BigDecimal proratedEntitlement(BigDecimal annualEntitlement, boolean prorationEnabled,
                                          LocalDate joiningDate, int year) {
        if (joiningDate.getYear() > year) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY);
        }
        if (!prorationEnabled) {
            return annualEntitlement.setScale(1, RoundingMode.HALF_UP);
        }
        BigDecimal exact = annualEntitlement
                .multiply(BigDecimal.valueOf(eligibleMonths(joiningDate, year)))
                .divide(MONTHS_PER_YEAR, 4, RoundingMode.HALF_UP);
        return roundToHalfDay(exact);
    }

    private BigDecimal roundToHalfDay(BigDecimal value) {
        return value.multiply(TWO).setScale(0, RoundingMode.HALF_UP).divide(TWO, 1, RoundingMode.UNNECESSARY);
    }
}
