package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank String refreshToken,
        @NotBlank String deviceId
) {}