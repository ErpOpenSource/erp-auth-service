package com.erp.auth.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    public static final String HEADER_REQUEST_ID = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_HTTP_METHOD = "httpMethod";
    public static final String MDC_HTTP_PATH = "httpPath";
    public static final String MDC_CLIENT_IP = "clientIp";
    public static final String MDC_USER_AGENT = "userAgent";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startNs = System.nanoTime();
        String requestId = request.getHeader(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        String clientIp = firstForwardedIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            MDC.put(MDC_REQUEST_ID, requestId);
            MDC.put(MDC_HTTP_METHOD, request.getMethod());
            MDC.put(MDC_HTTP_PATH, request.getRequestURI());
            if (clientIp != null && !clientIp.isBlank()) {
                MDC.put(MDC_CLIENT_IP, clientIp);
            }
            if (userAgent != null && !userAgent.isBlank()) {
                MDC.put(MDC_USER_AGENT, userAgent);
            }

            response.setHeader(HEADER_REQUEST_ID, requestId);

            filterChain.doFilter(request, response);
            long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
            log.info("HTTP {} {} -> {} ({} ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
        } finally {
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_HTTP_METHOD);
            MDC.remove(MDC_HTTP_PATH);
            MDC.remove(MDC_CLIENT_IP);
            MDC.remove(MDC_USER_AGENT);
        }
    }

    private static String firstForwardedIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] parts = forwardedFor.split(",");
            if (parts.length > 0) {
                return parts[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
