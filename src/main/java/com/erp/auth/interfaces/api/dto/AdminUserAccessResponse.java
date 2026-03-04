package com.erp.auth.interfaces.api.dto;

import java.util.List;
import java.util.UUID;

public record AdminUserAccessResponse(
        UUID userId,
        List<String> roles,
        List<String> modules,
        List<String> departments,
        List<String> permissions
) {}
