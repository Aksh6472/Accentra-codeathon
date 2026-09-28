package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.TestData;
import com.accentra.leavemanagement.entity.Holiday;
import com.accentra.leavemanagement.entity.LeavePolicy;
import com.accentra.leavemanagement.repository.HolidayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveDayCalculationServiceTest {

    // 2026-10-05 is a Monday; 2026-10-10/11 is a weekend.
    private static final LocalDate MON = LocalDate.of(2026, 10, 5);

    @Mock
    private HolidayRepository holidayRepository;
    @InjectMocks
    private LeaveDayCalculationService service;

    @Test
    void countsNormalWeekdays() {
        LeaveDayCalculation calc = service.calculate(MON, MON.plusDays(4), Set.of(), false, false);

        assertThat(calc.calendarDays()).isEqualTo(5);
        assertThat(calc.chargeableDays()).isEqualTo(5);
        assertThat(calc.weekendDays()).isZero();
    }

    @Test
    void singleDayLeave() {
        assertThat(service.calculate(MON, MON, Set.of(), false, false).chargeableDays()).isEqualTo(1);
    }

    @Test
    void excludesWeekends() {
        // Thu → next Tue: Thu, Fri, (Sat, Sun), Mon, Tue
        LeaveDayCalculation calc = service.calculate(MON.plusDays(3), MON.plusDays(8), Set.of(), false, false);

        assertThat(calc.calendarDays()).isEqualTo(6);
        assertThat(calc.weekendDays()).isEqualTo(2);
        assertThat(calc.chargeableDays()).isEqualTo(4);
    }

    @Test
    void weekendOnlyRangeHasNoChargeableDays() {
        assertThat(service.calculate(MON.plusDays(5), MON.plusDays(6), Set.of(), false, false).chargeableDays())
                .isZero();
    }

    @Test
    void excludesPublicHolidays() {
        LeaveDayCalculation calc = service.calculate(MON, MON.plusDays(4), Set.of(MON.plusDays(2)), false, false);

        assertThat(calc.holidayDays()).isEqualTo(1);
        assertThat(calc.chargeableDays()).isEqualTo(4);
        assertThat(calc.chargeableDates()).doesNotContain(MON.plusDays(2));
    }

    @Test
    void weekendAndHolidayCombinationIsNotDoubleCounted() {
        // Mon..next Mon with a holiday on Friday and another on Saturday (holiday on a weekend is a weekend day).
        Set<LocalDate> holidays = Set.of(MON.plusDays(4), MON.plusDays(5));
        LeaveDayCalculation calc = service.calculate(MON, MON.plusDays(7), holidays, false, false);

        assertThat(calc.calendarDays()).isEqualTo(8);
        assertThat(calc.weekendDays()).isEqualTo(2);
        assertThat(calc.holidayDays()).isEqualTo(1);
        assertThat(calc.chargeableDays()).isEqualTo(5);
    }

    @Test
    void policyCanChargeWeekendsAndHolidays() {
        Set<LocalDate> holidays = Set.of(MON.plusDays(4));
        assertThat(service.calculate(MON, MON.plusDays(6), holidays, true, false).chargeableDays()).isEqualTo(6);
        assertThat(service.calculate(MON, MON.plusDays(6), holidays, false, true).chargeableDays()).isEqualTo(5);
        assertThat(service.calculate(MON, MON.plusDays(6), holidays, true, true).chargeableDays()).isEqualTo(7);
    }

    @Test
    void usesConfiguredHolidaysFromTheDatabase() {
        LeavePolicy policy = TestData.policy(TestData.leaveType(1, "CASUAL"), "12");
        when(holidayRepository.findByDateBetweenOrderByDate(MON, MON.plusDays(4)))
                .thenReturn(List.of(new Holiday("Founders Day", MON.plusDays(1))));

        assertThat(service.calculate(MON, MON.plusDays(4), policy).chargeableDays()).isEqualTo(4);
    }

    @Test
    void rejectsReversedRange() {
        assertThatThrownBy(() -> service.calculate(MON, MON.minusDays(1), Set.of(), false, false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
