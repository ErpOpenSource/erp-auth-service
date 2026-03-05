package com.erp.auth.interfaces.api.dto;

import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        String status,
        List<String> roles,
        List<String> modules,
        List<String> departments,
        List<String> permissions
) {}
