package com.erp.auth.interfaces.api.errors;

import com.erp.auth.infrastructure.observability.RequestIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }
        ensureRequestIdHeader(response);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), ErrorResponseFactory.accessDenied());
    }

    private static void ensureRequestIdHeader(HttpServletResponse response) {
        if (response.getHeader(RequestIdFilter.HEADER_REQUEST_ID) != null) {
            return;
        }
        String requestId = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
        if (requestId != null && !requestId.isBlank()) {
            response.setHeader(RequestIdFilter.HEADER_REQUEST_ID, requestId);
        }
    }
}
