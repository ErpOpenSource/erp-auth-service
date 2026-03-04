package com.erp.auth.interfaces.api.errors;

import java.util.Map;

public record ApiErrorResponse(
        String code,
        String message,
        String requestId,
        String traceId,
        Map<String, Object> details
) {}
