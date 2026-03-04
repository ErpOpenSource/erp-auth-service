package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminUpdateUserRequest(
        @NotBlank @Size(max = 120) String username,
        @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 16) String status,
        List<String> roles,
        List<String> modules,
        List<String> departments
) {}
