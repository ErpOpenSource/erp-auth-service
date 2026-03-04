package com.erp.auth.interfaces.api.errors;

import org.springframework.http.HttpStatus;

public class UserDisabledException extends ApiException {
    public UserDisabledException() {
        super(ErrorCode.USER_DISABLED, HttpStatus.FORBIDDEN, "User is disabled.");
    }
}