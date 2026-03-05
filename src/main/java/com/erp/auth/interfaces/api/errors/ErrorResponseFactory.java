package com.erp.auth.interfaces.api.errors;

import org.slf4j.MDC;

import java.util.Map;

public final class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    public static ApiErrorResponse fromApiException(ApiException ex) {
        return new ApiErrorResponse(
                ex.getCode().name(),
                ex.getMessage(),
                mdc("requestId"),
                mdc("traceId"),
                ex.getDetails()
        );
    }

    public static ApiErrorResponse validation(Map<String, Object> details) {
        return new ApiErrorResponse(
                ErrorCode.VALIDATION_ERROR.name(),
                "Validation error.",
                mdc("requestId"),
                mdc("traceId"),
                details
        );
    }

    public static ApiErrorResponse unauthorized() {
        return new ApiErrorResponse(
                ErrorCode.UNAUTHORIZED.name(),
                "Authentication required.",
                mdc("requestId"),
                mdc("traceId"),
                null
        );
    }

    public static ApiErrorResponse accessDenied() {
        return new ApiErrorResponse(
                ErrorCode.ACCESS_DENIED.name(),
                "Access denied.",
                mdc("requestId"),
                mdc("traceId"),
                null
        );
    }

    public static ApiErrorResponse notFound() {
        return new ApiErrorResponse(
                ErrorCode.NOT_FOUND.name(),
                "Resource not found.",
                mdc("requestId"),
                mdc("traceId"),
                null
        );
    }

    public static ApiErrorResponse internal() {
        return new ApiErrorResponse(
                ErrorCode.INTERNAL_ERROR.name(),
                "Unexpected error.",
                mdc("requestId"),
                mdc("traceId"),
                null
        );
    }

    private static String mdc(String key) {
        String value = MDC.get(key);
        return (value == null || value.isBlank()) ? null : value;
    }
}
