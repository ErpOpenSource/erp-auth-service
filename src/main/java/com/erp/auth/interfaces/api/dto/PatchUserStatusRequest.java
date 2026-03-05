package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PatchUserStatusRequest(
        @NotBlank String status
) {}
