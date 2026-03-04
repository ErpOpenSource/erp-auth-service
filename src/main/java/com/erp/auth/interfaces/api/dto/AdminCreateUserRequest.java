package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AdminCreateUserRequest(
        @NotBlank String username,
        @Email String email,
        @NotBlank @Size(min = 8, max = 200) String password,
        List<String> roles,
        List<String> modules,
        List<String> departments
) {}
