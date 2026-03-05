package com.erp.auth.application.auth;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.erp.auth.infrastructure.security.PasswordHasher;
import com.erp.auth.interfaces.api.dto.ChangePasswordRequest;
import com.erp.auth.interfaces.api.dto.UpdateUserProfileRequest;
import com.erp.auth.interfaces.api.dto.UserProfileResponse;
import com.erp.auth.interfaces.api.errors.ApiException;
import com.erp.auth.interfaces.api.errors.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class UserProfileUseCase {

    private final UserJpaRepository userRepo;
    private final UserAuthorizationService userAuthorizationService;
    private final PasswordHasher passwordHasher;
    private final AuditService auditService;

    public UserProfileUseCase(
            UserJpaRepository userRepo,
            UserAuthorizationService userAuthorizationService,
            PasswordHasher passwordHasher,
            AuditService auditService
    ) {
        this.userRepo = userRepo;
        this.userAuthorizationService = userAuthorizationService;
        this.passwordHasher = passwordHasher;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        UserEntity user = requireUser(userId);
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(
            UUID userId,
            UpdateUserProfileRequest request,
            String ip,
            String userAgent
    ) {
        UserEntity user = requireUser(userId);

        String username = request.username() == null ? "" : request.username().trim();
        String email = request.email() == null ? "" : request.email().trim();

        if (username.isBlank()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Username is required.",
                    Map.of("field", "username")
            );
        }
        if (email.isBlank()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Email is required.",
                    Map.of("field", "email")
            );
        }

        userRepo.findByUsername(username)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ApiException(
                            ErrorCode.VALIDATION_ERROR,
                            HttpStatus.CONFLICT,
                            "Username already exists.",
                            Map.of("field", "username")
                    );
                });

        userRepo.findByEmail(email)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ApiException(
                            ErrorCode.VALIDATION_ERROR,
                            HttpStatus.CONFLICT,
                            "Email already exists.",
                            Map.of("field", "email")
                    );
                });

        boolean changed = false;
        if (!username.equals(user.getUsername())) {
            user.setUsername(username);
            changed = true;
        }
        if (!Objects.equals(email, user.getEmail())) {
            user.setEmail(email);
            changed = true;
        }
        if (changed) {
            user.setUpdatedAt(OffsetDateTime.now());
            userRepo.save(user);
        }

        auditService.record(
                "USER_PROFILE_UPDATE",
                user,
                user,
                null,
                "{\"changed\":" + changed
                        + ",\"ip\":\"" + safe(ip) + "\""
                        + ",\"userAgent\":\"" + safe(userAgent) + "\"}"
        );

        return toResponse(user);
    }

    @Transactional
    public void changePassword(
            UUID userId,
            ChangePasswordRequest request,
            String ip,
            String userAgent
    ) {
        UserEntity user = requireUser(userId);

        if (!passwordHasher.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect.",
                    Map.of("field", "currentPassword")
            );
        }

        user.setPasswordHash(passwordHasher.hash(request.newPassword()));
        user.setUpdatedAt(OffsetDateTime.now());
        userRepo.save(user);

        auditService.record(
                "USER_PASSWORD_CHANGE",
                user,
                user,
                null,
                "{\"result\":\"UPDATED\""
                        + ",\"ip\":\"" + safe(ip) + "\""
                        + ",\"userAgent\":\"" + safe(userAgent) + "\"}"
        );
    }

    private UserEntity requireUser(UUID userId) {
        return userRepo.findById(userId).orElseThrow(() -> new ApiException(
                ErrorCode.UNAUTHORIZED,
                HttpStatus.UNAUTHORIZED,
                "Authentication required.",
                null
        ));
    }

    private UserProfileResponse toResponse(UserEntity user) {
        UserAuthorizationContext authorization = userAuthorizationService.resolveForUser(user.getId());
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus().name(),
                authorization.roles(),
                authorization.modules(),
                authorization.departments(),
                authorization.permissions()
        );
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "'");
    }
}
