package com.accentra.leavemanagement.dto;

import java.time.Instant;

public record LoginResponse(String token, String tokenType, Instant expiresAt, EmployeeProfileResponse user) {
}
