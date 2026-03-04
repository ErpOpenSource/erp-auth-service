package com.erp.auth.interfaces.api.dto;

import java.util.List;

public record UserProfileResponse(
        String id,
        String username,
        String email,
        String status,
        List<String> roles,
        List<String> modules,
        List<String> permissions
) {}
