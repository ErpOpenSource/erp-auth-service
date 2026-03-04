package com.erp.auth.interfaces.api.errors;

import com.erp.auth.infrastructure.observability.RequestIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public SecurityAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        if (response.isCommitted()) {
            return;
        }
        ensureRequestIdHeader(response);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), ErrorResponseFactory.unauthorized());
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
