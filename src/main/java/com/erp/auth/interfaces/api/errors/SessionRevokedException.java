package com.erp.auth.interfaces.api.errors;

import org.springframework.http.HttpStatus;

public class SessionRevokedException extends ApiException {
    public SessionRevokedException() {
        super(ErrorCode.SESSION_REVOKED, HttpStatus.UNAUTHORIZED, "Session is revoked or expired.");
    }
}