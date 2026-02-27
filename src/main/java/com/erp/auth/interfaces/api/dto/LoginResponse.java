package com.erp.auth.interfaces.api.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        long refreshTokenExpiresInSeconds,
        UUID sessionId,
        UserSummary user
) {
    public record UserSummary(
            UUID id,
            String username,
            String status,
            List<String> roles
    ) {}
}