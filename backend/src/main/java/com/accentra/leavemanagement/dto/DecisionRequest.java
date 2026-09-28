package com.accentra.leavemanagement.dto;

import jakarta.validation.constraints.Size;

public record DecisionRequest(@Size(max = 1000, message = "Comment must be at most 1000 characters") String comment) {

    public static final DecisionRequest EMPTY = new DecisionRequest(null);
}
