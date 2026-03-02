package com.erp.auth.interfaces.api;

import com.erp.auth.application.auth.LoginCommand;
import com.erp.auth.application.auth.LoginUseCase;
import com.erp.auth.application.auth.RefreshCommand;
import com.erp.auth.application.auth.RefreshUseCase;
import com.erp.auth.interfaces.api.dto.LoginRequest;
import com.erp.auth.interfaces.api.dto.LoginResponse;
import com.erp.auth.interfaces.api.dto.RefreshRequest;
import com.erp.auth.interfaces.api.dto.RefreshResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshUseCase refreshUseCase;

    public AuthController(LoginUseCase loginUseCase, RefreshUseCase refreshUseCase) {
        this.loginUseCase = loginUseCase;
        this.refreshUseCase = refreshUseCase;
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
}
