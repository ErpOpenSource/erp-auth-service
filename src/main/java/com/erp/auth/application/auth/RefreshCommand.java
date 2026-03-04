package com.erp.auth.application.auth;

public record RefreshCommand(
        String refreshToken,
        String deviceId,
        String ip,
        String userAgent
) {}