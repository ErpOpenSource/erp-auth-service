package com.erp.auth.interfaces.api.dto;

import java.time.LocalDateTime;

public record SessionSummaryResponse(
        String id,
        String userId,
        String username,
        String ip,
        String userAgent,
        LocalDateTime createdAt,
        LocalDateTime lastSeenAt,
        LocalDateTime expiresAt
) {}
