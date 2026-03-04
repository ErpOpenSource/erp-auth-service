package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreatePermissionRequest(
        @NotBlank @Size(max = 128) String code,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 64) String moduleCode
) {}
