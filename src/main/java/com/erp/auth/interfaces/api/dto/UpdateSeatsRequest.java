package com.erp.auth.interfaces.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateSeatsRequest(
        @Min(0) int maxSeats,
        @NotBlank String enforceMode
) {}
