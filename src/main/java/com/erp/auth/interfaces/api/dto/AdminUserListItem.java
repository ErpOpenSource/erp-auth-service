package com.erp.auth.interfaces.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserListItem(
        UUID id,
        String username,
        String email,
        String status,
        OffsetDateTime createdAt
) {}
