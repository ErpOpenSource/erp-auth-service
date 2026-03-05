package com.erp.auth.interfaces.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminDepartmentResponse(
        UUID id,
        String code,
        String name,
        OffsetDateTime createdAt
) {}
