package com.accentra.leavemanagement.service;

import com.accentra.leavemanagement.exception.InvalidDateRangeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Structural date rules for a leave application, shared by preview and submission. */
@Component
@RequiredArgsConstructor
public class LeaveApplicationValidator {

    static final int MAX_CALENDAR_DAYS = 90;
    static final int MAX_MONTHS_AHEAD = 12;

    private final Clock clock;

    public void validateDates(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now(clock);
        if (end.isBefore(start)) {
            throw new InvalidDateRangeException("End date cannot be before start date");
        }
        if (start.isBefore(today)) {
            throw new InvalidDateRangeException("Leave cannot start in the past");
        }
        if (start.getYear() != end.getYear()) {
            throw new InvalidDateRangeException(
                    "Leave cannot span two calendar years; submit one request per year");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_CALENDAR_DAYS) {
            throw new InvalidDateRangeException("A single request cannot exceed " + MAX_CALENDAR_DAYS + " calendar days");
        }
        if (start.isAfter(today.plusMonths(MAX_MONTHS_AHEAD))) {
            throw new InvalidDateRangeException(
                    "Leave can be requested at most " + MAX_MONTHS_AHEAD + " months in advance");
        }
    }
}
