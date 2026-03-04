package com.erp.auth.interfaces.api.dto;

import java.time.LocalDateTime;

public record UserSummaryResponse(
        String id,
        String username,
        String email,
        String status,
        LocalDateTime createdAt
) {}
