package com.accentra.leavemanagement.service;

import java.time.LocalDate;
import java.util.List;

/**
 * Result of counting leave days in an inclusive date range.
 *
 * @param calendarDays    every day in the range
 * @param chargeableDays  days deducted from the balance
 * @param weekendDays     Saturdays/Sundays in the range
 * @param holidayDays     public holidays falling on weekdays (a holiday on a weekend counts as a weekend day)
 * @param chargeableDates the individual chargeable dates
 */
public record LeaveDayCalculation(int calendarDays, int chargeableDays, int weekendDays, int holidayDays,
                                  List<LocalDate> chargeableDates) {
}
