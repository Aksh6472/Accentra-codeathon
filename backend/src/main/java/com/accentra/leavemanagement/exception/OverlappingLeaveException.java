package com.accentra.leavemanagement.exception;

import org.springframework.http.HttpStatus;

public class OverlappingLeaveException extends ApiException {

    public OverlappingLeaveException(String message) {
        super(HttpStatus.CONFLICT, "OVERLAPPING_LEAVE", message);
    }
}
