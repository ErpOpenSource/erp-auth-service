package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileRequest(
        @NotBlank String username,
        @Email @NotBlank String email,
        String department
) {}
