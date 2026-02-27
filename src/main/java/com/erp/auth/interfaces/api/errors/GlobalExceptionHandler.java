package com.erp.auth.interfaces.api.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        log.warn("API exception: code={}, status={}, message={}", ex.getCode(), ex.getHttpStatus().value(), ex.getMessage());
        ApiError body = new ApiError(
                ex.getCode().name(),
                ex.getMessage(),
                mdc("requestId"),
                mdc("traceId"),
                ex.getDetails()
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        details.put("fields", fieldErrors);
        log.warn("Validation error: {}", fieldErrors);

        ApiError body = new ApiError(
                ErrorCode.VALIDATION_ERROR.name(),
                "Validation error.",
                mdc("requestId"),
                mdc("traceId"),
                details
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        ApiException apiCause = findApiCause(ex);
        if (apiCause != null) {
            return handleApiException(apiCause);
        }

        log.error("Unexpected exception type={}", ex.getClass().getName(), ex);
        ApiError body = new ApiError(
                ErrorCode.INTERNAL_ERROR.name(),
                "Unexpected error.",
                mdc("requestId"),
                mdc("traceId"),
                null
        );
        return ResponseEntity.internalServerError().body(body);
    }

    private static String mdc(String key) {
        String v = MDC.get(key);
        return (v == null || v.isBlank()) ? null : v;
    }

    private static ApiException findApiCause(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ApiException apiException) {
                return apiException;
            }
            current = current.getCause();
        }
        return null;
    }
}
