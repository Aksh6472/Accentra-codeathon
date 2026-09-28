package com.accentra.leavemanagement.dto;

import com.accentra.leavemanagement.entity.Holiday;

import java.time.LocalDate;

public record HolidayResponse(Long id, String name, LocalDate date) {

    public static HolidayResponse from(Holiday holiday) {
        return new HolidayResponse(holiday.getId(), holiday.getName(), holiday.getDate());
    }
}
