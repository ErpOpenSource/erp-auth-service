package com.erp.auth.application.auth;

public record LoginCommand(
        String username,
        String password,
        String deviceId,
        boolean rememberMe,
        String ip,
        String userAgent
) {}