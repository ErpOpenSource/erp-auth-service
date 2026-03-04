package com.erp.auth.interfaces.api;

import com.erp.auth.application.auth.LoginCommand;
import com.erp.auth.application.auth.LoginUseCase;
import com.erp.auth.application.auth.LogoutAllCommand;
import com.erp.auth.application.auth.LogoutAllUseCase;
import com.erp.auth.application.auth.LogoutCommand;
import com.erp.auth.application.auth.LogoutUseCase;
import com.erp.auth.application.auth.RefreshCommand;
import com.erp.auth.application.auth.RefreshUseCase;
import com.erp.auth.interfaces.api.dto.LoginRequest;
import com.erp.auth.interfaces.api.dto.LoginResponse;
import com.erp.auth.interfaces.api.dto.LogoutRequest;
import com.erp.auth.interfaces.api.dto.RefreshRequest;
import com.erp.auth.interfaces.api.dto.RefreshResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshUseCase refreshUseCase;
    private final LogoutUseCase logoutUseCase;
    private final LogoutAllUseCase logoutAllUseCase;

    public AuthController(
            LoginUseCase loginUseCase,
            RefreshUseCase refreshUseCase,
            LogoutUseCase logoutUseCase,
            LogoutAllUseCase logoutAllUseCase
    ) {
        this.loginUseCase = loginUseCase;
        this.refreshUseCase = refreshUseCase;
        this.logoutUseCase = logoutUseCase;
        this.logoutAllUseCase = logoutAllUseCase;
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {

        String ip = http.getRemoteAddr();
        String ua = http.getHeader("User-Agent");

        boolean rememberMe = request.rememberMe() != null && request.rememberMe();

        return loginUseCase.execute(new LoginCommand(
                request.username(),
                request.password(),
                request.deviceId(),
                rememberMe,
                ip,
                ua
        ));
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {

        String ip = http.getRemoteAddr();
        String ua = http.getHeader("User-Agent");

        return refreshUseCase.execute(new RefreshCommand(
                request.refreshToken(),
                request.deviceId(),
                ip,
                ua
        ));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        String ua = http.getHeader("User-Agent");

        logoutUseCase.execute(new LogoutCommand(
                request.sessionId(),
                ip,
                ua
        ));
    }

    @PostMapping("/logout/all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal Jwt jwt, HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        String ua = http.getHeader("User-Agent");
        UUID userId = UUID.fromString(jwt.getSubject());

        logoutAllUseCase.execute(new LogoutAllCommand(
                userId,
                ip,
                ua
        ));
    }
}
