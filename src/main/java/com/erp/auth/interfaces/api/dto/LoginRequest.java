package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String deviceId,
        Boolean rememberMe
) {}