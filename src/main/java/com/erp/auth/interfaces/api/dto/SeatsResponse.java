package com.erp.auth.interfaces.api.dto;

public record SeatsResponse(
        int maxSeats,
        int activeSeats,
        String enforceMode
) {}