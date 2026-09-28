package com.accentra.leavemanagement.exception;

import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends ApiException {

    public InvalidDateRangeException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", message);
    }
}
