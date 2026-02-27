package com.erp.auth.interfaces.api;

import com.erp.auth.interfaces.api.dto.ActiveSessionsResponse;
import com.erp.auth.interfaces.api.dto.SeatsResponse;
import com.erp.auth.interfaces.api.errors.NotImplementedApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    @GetMapping("/licenses/seats")
    public SeatsResponse getSeats() {
        throw new NotImplementedApiException("admin/licenses/seats");
    }

    @GetMapping("/sessions/active")
    public ActiveSessionsResponse getActiveSessions() {
        throw new NotImplementedApiException("admin/sessions/active");
    }

    @PostMapping("/sessions/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(@PathVariable("id") UUID sessionId) {
        throw new NotImplementedApiException("admin/sessions/{id}/revoke");
    }
}