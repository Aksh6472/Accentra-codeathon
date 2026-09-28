package com.accentra.leavemanagement.dto;

public record TeamSummaryResponse(Long id, String name, String description, Long managerId, String managerName,
                                  long memberCount) {
}
