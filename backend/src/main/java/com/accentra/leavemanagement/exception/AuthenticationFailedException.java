package com.accentra.leavemanagement.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationFailedException extends ApiException {

    public AuthenticationFailedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", message);
    }
}
