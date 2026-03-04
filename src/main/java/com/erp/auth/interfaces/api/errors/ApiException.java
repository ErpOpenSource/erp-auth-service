package com.erp.auth.interfaces.api.errors;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus httpStatus;
    private final Map<String, Object> details;

    public ApiException(ErrorCode code, HttpStatus httpStatus, String message) {
        this(code, httpStatus, message, null);
    }

    public ApiException(ErrorCode code, HttpStatus httpStatus, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.details = details;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}