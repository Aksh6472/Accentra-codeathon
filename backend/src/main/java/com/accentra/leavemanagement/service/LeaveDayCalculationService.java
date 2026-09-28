package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.entity.Holiday;
import com.accentra.leavemanagement.entity.LeavePolicy;
import com.accentra.leavemanagement.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single source of truth for turning a date range into leave days.
 * <p>
 * Default rule: Saturdays, Sundays and configured public holidays are not charged. A leave policy
 * can opt in to charging weekends and/or holidays via {@code countWeekends}/{@code countHolidays}.
 */
@Service
@RequiredArgsConstructor
public class LeaveDayCalculationService {

    private final HolidayRepository holidayRepository;

    @Transactional(readOnly = true)
    public LeaveDayCalculation calculate(LocalDate start, LocalDate end, LeavePolicy policy) {
        return calculate(start, end, holidayDates(start, end), policy.isCountWeekends(), policy.isCountHolidays());
    }

    public LeaveDayCalculation calculate(LocalDate start, LocalDate end, Set<LocalDate> holidays,
                                         boolean countWeekends, boolean countHolidays) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date must not be before start date");
        }
        int calendarDays = 0;
        int weekendDays = 0;
        int holidayDays = 0;
        List<LocalDate> chargeable = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            calendarDays++;
            if (isWeekend(day)) {
                weekendDays++;
                if (countWeekends) {
                    chargeable.add(day);
                }
            } else if (holidays.contains(day)) {
                holidayDays++;
                if (countHolidays) {
                    chargeable.add(day);
                }
            } else {
                chargeable.add(day);
            }
        }
        return new LeaveDayCalculation(calendarDays, chargeable.size(), weekendDays, holidayDays, List.copyOf(chargeable));
    }

    /** Days the organisation is open (not a weekend, not a holiday). Used for team availability. */
    @Transactional(readOnly = true)
    public List<LocalDate> workingDays(LocalDate start, LocalDate end) {
        return calculate(start, end, holidayDates(start, end), false, false).chargeableDates();
    }

    @Transactional(readOnly = true)
    public List<Holiday> holidaysBetween(LocalDate start, LocalDate end) {
        return holidayRepository.findByDateBetweenOrderByDate(start, end);
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, String> holidayNames(LocalDate start, LocalDate end) {
        return holidaysBetween(start, end).stream().collect(Collectors.toMap(Holiday::getDate, Holiday::getName));
    }

    public boolean isWeekend(LocalDate day) {
        DayOfWeek dow = day.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    private Set<LocalDate> holidayDates(LocalDate start, LocalDate end) {
        return holidaysBetween(start, end).stream().map(Holiday::getDate).collect(Collectors.toSet());
    }
}
