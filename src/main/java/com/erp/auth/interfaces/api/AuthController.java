package com.erp.auth.interfaces.api;

import com.erp.auth.interfaces.api.dto.*;
import com.erp.auth.interfaces.api.errors.NotImplementedApiException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        throw new NotImplementedApiException("login");
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        throw new NotImplementedApiException("refresh");
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        throw new NotImplementedApiException("logout");
    }

    @PostMapping("/logout/all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll() {
        throw new NotImplementedApiException("logout/all");
    }
}