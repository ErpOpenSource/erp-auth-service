package com.erp.auth.interfaces.api;

import com.erp.auth.application.admin.LicenseService;
import com.erp.auth.application.admin.SessionService;
import com.erp.auth.application.user.UserService;
import com.erp.auth.interfaces.api.dto.LicenseStatusResponse;
import com.erp.auth.interfaces.api.dto.SessionSummaryResponse;
import com.erp.auth.interfaces.api.dto.UserSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final SessionService sessionService;
    private final LicenseService licenseService;

    public AdminController(
            UserService userService,
            SessionService sessionService,
            LicenseService licenseService
    ) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.licenseService = licenseService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryResponse>> listUsers() {
        return ResponseEntity.ok(userService.listAll());
    }

    @GetMapping("/license")
    public ResponseEntity<LicenseStatusResponse> getLicenseStatus() {
        return ResponseEntity.ok(licenseService.getStatus());
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionSummaryResponse>> listSessions() {
        return ResponseEntity.ok(sessionService.listActiveSessions());
    }
}
