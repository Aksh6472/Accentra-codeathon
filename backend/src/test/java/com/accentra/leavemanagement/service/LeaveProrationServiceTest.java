package com.accentra.leavemanagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveProrationServiceTest {

    private final LeaveProrationService service = new LeaveProrationService();
    private static final BigDecimal ANNUAL = new BigDecimal("24");

    @Test
    void fullYearEmployeeGetsFullEntitlement() {
        assertThat(service.proratedEntitlement(ANNUAL, true, LocalDate.of(2020, 3, 10), 2026))
                .isEqualByComparingTo("24");
    }

    @Test
    void employeeJoiningOnJulyFirstGetsHalf() {
        assertThat(service.eligibleMonths(LocalDate.of(2026, 7, 1), 2026)).isEqualTo(6);
        assertThat(service.proratedEntitlement(ANNUAL, true, LocalDate.of(2026, 7, 1), 2026))
                .isEqualByComparingTo("12");
    }

    @ParameterizedTest(name = "joined {0} -> {1} months, {2} days of 24")
    @CsvSource({
            "2026-01-01, 12, 24",
            "2026-01-15, 12, 24",   // on the cut-off day: January counts
            "2026-01-16, 11, 22",   // after the cut-off: accrual starts in February
            "2026-07-20, 5, 10",
            "2026-12-01, 1, 2",
            "2026-12-31, 0, 0",
    })
    void midYearJoinersAreProratedByMonth(LocalDate joined, int months, String expected) {
        assertThat(service.eligibleMonths(joined, 2026)).isEqualTo(months);
        assertThat(service.proratedEntitlement(ANNUAL, true, joined, 2026)).isEqualByComparingTo(expected);
    }

    @Test
    void roundsToNearestHalfDay() {
        // 10 × 5/12 = 4.17 → 4.0 ; 10 × 7/12 = 5.83 → 6.0 ; 15 × 3/12 = 3.75 → 4.0
        assertThat(service.proratedEntitlement(new BigDecimal("10"), true, LocalDate.of(2026, 8, 1), 2026))
                .isEqualByComparingTo("4.0");
        assertThat(service.proratedEntitlement(new BigDecimal("10"), true, LocalDate.of(2026, 6, 1), 2026))
                .isEqualByComparingTo("6.0");
        assertThat(service.proratedEntitlement(new BigDecimal("15"), true, LocalDate.of(2026, 10, 1), 2026))
                .isEqualByComparingTo("4.0");
        // 9 × 7 / 12 = 5.25 → 5.5 (half-up at the half-day step)
        assertThat(service.proratedEntitlement(new BigDecimal("9"), true, LocalDate.of(2026, 6, 1), 2026))
                .isEqualByComparingTo("5.5");
    }

    @Test
    void employeeJoiningAfterTheYearGetsNothing() {
        assertThat(service.proratedEntitlement(ANNUAL, true, LocalDate.of(2027, 2, 1), 2026)).isEqualByComparingTo("0");
    }

    @Test
    void policyWithoutProrationGrantsFullEntitlementToMidYearJoiners() {
        assertThat(service.proratedEntitlement(ANNUAL, false, LocalDate.of(2026, 9, 1), 2026))
                .isEqualByComparingTo("24");
    }
}
