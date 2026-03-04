package com.erp.auth.application.auth;

import java.util.UUID;

public record LogoutAllCommand(
        UUID userId,
        String ip,
        String userAgent
) {}
