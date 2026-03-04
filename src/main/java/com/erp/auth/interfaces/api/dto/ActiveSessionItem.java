package com.erp.auth.interfaces.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ActiveSessionItem(
        UUID sessionId,
        UUID userId,
        String username,
        String deviceId,
        OffsetDateTime createdAt,
        OffsetDateTime lastSeenAt,
        OffsetDateTime expiresAt
) {}