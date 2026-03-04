package com.erp.auth.interfaces.api.errors;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class SeatLimitReachedException extends ApiException {
    public SeatLimitReachedException(int maxSeats, int activeSeats) {
        super(
            ErrorCode.SEAT_LIMIT_REACHED,
            HttpStatus.CONFLICT,
            "Concurrent seat limit reached.",
            Map.of("maxSeats", maxSeats, "activeSeats", activeSeats)
        );
    }
}
