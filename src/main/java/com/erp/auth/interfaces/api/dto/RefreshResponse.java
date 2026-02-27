package com.erp.auth.interfaces.api.dto;

import java.util.UUID;

public record RefreshResponse(
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        long refreshTokenExpiresInSeconds,
        UUID sessionId
) {}