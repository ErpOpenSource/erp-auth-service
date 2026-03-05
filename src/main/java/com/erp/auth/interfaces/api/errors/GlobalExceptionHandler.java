package com.erp.auth.interfaces.api.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex) {
        log.warn("API exception: code={}, status={}, message={}", ex.getCode(), ex.getHttpStatus().value(), ex.getMessage());
        ApiErrorResponse body = ErrorResponseFactory.fromApiException(ex);
        return ResponseEntity.status(ex.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        details.put("fields", fieldErrors);
        log.warn("Validation error: {}", fieldErrors);

        ApiErrorResponse body = ErrorResponseFactory.validation(details);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NoResourceFoundException ex) {
        log.warn("No resource found for path={}", ex.getResourcePath());
        ApiErrorResponse body = ErrorResponseFactory.notFound();
        return ResponseEntity.status(404).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        ApiException apiCause = findApiCause(ex);
        if (apiCause != null) {
            return handleApiException(apiCause);
        }

        log.error("Unexpected exception type={}", ex.getClass().getName(), ex);
        ApiErrorResponse body = ErrorResponseFactory.internal();
        return ResponseEntity.internalServerError().body(body);
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
