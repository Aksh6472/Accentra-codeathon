package com.accentra.leavemanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateHolidayRequest(
        @NotBlank(message = "Name is required") @Size(max = 100) String name,
        @NotNull(message = "Date is required") LocalDate date) {
}
