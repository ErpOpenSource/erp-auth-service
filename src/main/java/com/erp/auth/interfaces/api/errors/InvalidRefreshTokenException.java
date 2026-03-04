package com.erp.auth.interfaces.api.errors;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends ApiException {
    public InvalidRefreshTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN, HttpStatus.UNAUTHORIZED, "Invalid refresh token.");
    }
}