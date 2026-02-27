package com.erp.auth.interfaces.api.errors;

import org.springframework.http.HttpStatus;

public class RefreshReuseDetectedException extends ApiException {
    public RefreshReuseDetectedException() {
        super(ErrorCode.REFRESH_REUSE_DETECTED, HttpStatus.UNAUTHORIZED, "Refresh token reuse detected.");
    }
}