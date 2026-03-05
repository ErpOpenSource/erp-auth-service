package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name
) {}
