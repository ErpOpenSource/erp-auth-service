package com.erp.auth.interfaces.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LegacyAdminSessionResponse(
        UUID id,
        UUID userId,
        String username,
        String deviceId,
        String ip,
        String userAgent,
        OffsetDateTime createdAt,
        OffsetDateTime lastSeenAt,
        OffsetDateTime expiresAt,
        boolean current
) {}
