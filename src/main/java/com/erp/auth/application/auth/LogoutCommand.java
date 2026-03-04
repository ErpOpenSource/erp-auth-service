package com.erp.auth.application.auth;

import java.util.UUID;

public record LogoutCommand(
        UUID sessionId,
        String ip,
        String userAgent
) {}
