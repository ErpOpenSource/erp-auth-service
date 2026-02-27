package com.erp.auth.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_HTTP_METHOD = "httpMethod";
    public static final String MDC_HTTP_PATH = "httpPath";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        try {
            MDC.put(MDC_REQUEST_ID, requestId);
            MDC.put(MDC_HTTP_METHOD, request.getMethod());
            MDC.put(MDC_HTTP_PATH, request.getRequestURI());

            response.setHeader(HEADER_REQUEST_ID, requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_HTTP_METHOD);
            MDC.remove(MDC_HTTP_PATH);
        }
    }
}