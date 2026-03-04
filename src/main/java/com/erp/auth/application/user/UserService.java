package com.erp.auth.application.user;

import com.erp.auth.application.auth.UserAuthorizationContext;
import com.erp.auth.application.auth.UserAuthorizationService;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.erp.auth.interfaces.api.dto.UserProfileResponse;
import com.erp.auth.interfaces.api.dto.UserSummaryResponse;
import com.erp.auth.interfaces.api.errors.ApiException;
import com.erp.auth.interfaces.api.errors.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final UserJpaRepository userRepo;
    private final UserAuthorizationService userAuthorizationService;

    public UserService(
            UserJpaRepository userRepo,
            UserAuthorizationService userAuthorizationService
    ) {
        this.userRepo = userRepo;
        this.userAuthorizationService = userAuthorizationService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String userId) {
        UUID parsedUserId;
        try {
            parsedUserId = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(
                    ErrorCode.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user id.",
                    Map.of("userId", userId)
            );
        }

        UserEntity user = userRepo.findById(parsedUserId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.UNAUTHORIZED,
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."
                ));

        UserAuthorizationContext context = userAuthorizationService.resolveForUser(parsedUserId);
        return new UserProfileResponse(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus().name(),
                context.roles(),
                context.modules(),
                context.permissions()
        );
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> listAll() {
        return userRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(user -> new UserSummaryResponse(
                        user.getId().toString(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getStatus().name(),
                        user.getCreatedAt().toLocalDateTime()
                ))
                .toList();
    }
}
