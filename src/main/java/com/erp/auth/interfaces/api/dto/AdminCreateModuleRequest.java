package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateModuleRequest(
        @NotBlank @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name
) {}
