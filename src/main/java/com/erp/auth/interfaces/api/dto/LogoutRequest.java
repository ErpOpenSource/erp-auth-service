package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LogoutRequest(
        @NotNull UUID sessionId
) {}