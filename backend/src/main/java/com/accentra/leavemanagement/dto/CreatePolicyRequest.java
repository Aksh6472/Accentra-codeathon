package com.accentra.leavemanagement.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePolicyRequest(
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{1,29}$", message = "Code must be 2-30 letters, digits or underscores")
        String code,
        @NotBlank(message = "Name is required") @Size(max = 60) String name,
        @Size(max = 255) String description,
        @NotNull(message = "Annual entitlement is required")
        @DecimalMin(value = "0", message = "Annual entitlement cannot be negative")
        @DecimalMax(value = "365", message = "Annual entitlement cannot exceed 365 days")
        @Digits(integer = 3, fraction = 1, message = "Use at most one decimal place")
        BigDecimal annualEntitlement,
        boolean prorated,
        boolean countWeekends,
        boolean countHolidays) {
}
