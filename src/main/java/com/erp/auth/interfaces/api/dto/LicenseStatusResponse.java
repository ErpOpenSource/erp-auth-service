package com.erp.auth.interfaces.api.dto;

public record LicenseStatusResponse(
        int maxConcurrentSeats,
        int usedSeats,
        String enforceMode
) {}
