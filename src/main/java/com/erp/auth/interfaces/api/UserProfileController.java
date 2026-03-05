package com.erp.auth.interfaces.api;

import com.erp.auth.application.auth.UserProfileUseCase;
import com.erp.auth.interfaces.api.dto.ChangePasswordRequest;
import com.erp.auth.interfaces.api.dto.UpdateUserProfileRequest;
import com.erp.auth.interfaces.api.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users/me")
public class UserProfileController {

    private final UserProfileUseCase userProfileUseCase;

    public UserProfileController(UserProfileUseCase userProfileUseCase) {
        this.userProfileUseCase = userProfileUseCase;
    }

    @GetMapping
    public UserProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userProfileUseCase.getProfile(userId(jwt));
    }

    @PutMapping
    public UserProfileResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserProfileRequest request,
            HttpServletRequest http
    ) {
        return userProfileUseCase.updateProfile(
                userId(jwt),
                request,
                http.getRemoteAddr(),
                http.getHeader("User-Agent")
        );
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest http
    ) {
        userProfileUseCase.changePassword(
                userId(jwt),
                request,
                http.getRemoteAddr(),
                http.getHeader("User-Agent")
        );
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
