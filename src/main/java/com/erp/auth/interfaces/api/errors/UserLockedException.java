package com.erp.auth.interfaces.api.errors;

import org.springframework.http.HttpStatus;

public class UserLockedException extends ApiException {
    public UserLockedException() {
        super(ErrorCode.USER_LOCKED, HttpStatus.FORBIDDEN, "User is locked.");
    }
}