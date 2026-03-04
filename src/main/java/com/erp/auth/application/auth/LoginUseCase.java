package com.erp.auth.application.auth;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.infrastructure.persistence.jpa.entity.LicenseSeatsEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.SessionEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.LicenseSeatsJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.erp.auth.infrastructure.security.JwtService;
import com.erp.auth.infrastructure.security.PasswordHasher;
import com.erp.auth.infrastructure.security.RefreshTokenService;
import com.erp.auth.interfaces.api.dto.LoginResponse;
import com.erp.auth.interfaces.api.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class LoginUseCase {

    private final UserJpaRepository userRepo;
    private final SessionJpaRepository sessionRepo;
    private final LicenseSeatsJpaRepository seatsRepo;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final UserAuthorizationService userAuthorizationService;

    private final long accessTokenMinutes;
    private final int refreshDaysDefault;
    private final int refreshDaysRememberMe;

    public LoginUseCase(
            UserJpaRepository userRepo,
            SessionJpaRepository sessionRepo,
            LicenseSeatsJpaRepository seatsRepo,
            PasswordHasher passwordHasher,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            AuditService auditService,
            UserAuthorizationService userAuthorizationService,
            @Value("${auth.jwt.access-token-expiration-minutes}") long accessTokenMinutes,
            @Value("${auth.refresh.expiration-days-default}") int refreshDaysDefault,
            @Value("${auth.refresh.expiration-days-remember-me}") int refreshDaysRememberMe
    ) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.seatsRepo = seatsRepo;
        this.passwordHasher = passwordHasher;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.userAuthorizationService = userAuthorizationService;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshDaysDefault = refreshDaysDefault;
        this.refreshDaysRememberMe = refreshDaysRememberMe;
    }

    @Transactional
    public LoginResponse execute(LoginCommand cmd) {

        // 1) user lookup
        UserEntity user = userRepo.findByUsername(cmd.username())
                .orElseThrow(() -> {
                    auditService.record("LOGIN_FAILED", null, null, null,
                            "{\"reason\":\"INVALID_CREDENTIALS\",\"username\":\"" + safe(cmd.username()) + "\"}");
                    return new InvalidCredentialsException();
                });

        // 2) status checks
        if (user.getStatus() == UserEntity.UserStatus.LOCKED) {
            auditService.record("LOGIN_FAILED", user, user, null, "{\"reason\":\"USER_LOCKED\"}");
            throw new UserLockedException();
        }
        if (user.getStatus() == UserEntity.UserStatus.DISABLED) {
            auditService.record("LOGIN_FAILED", user, user, null, "{\"reason\":\"USER_DISABLED\"}");
            throw new UserDisabledException();
        }

        // 3) password verify
        if (!passwordHasher.matches(cmd.password(), user.getPasswordHash())) {
            auditService.record("LOGIN_FAILED", user, user, null, "{\"reason\":\"INVALID_CREDENTIALS\"}");
            throw new InvalidCredentialsException();
        }

        // 4) ENFORCEMENT (transactional + FOR UPDATE)
        LicenseSeatsEntity seats = seatsRepo.lockMainForUpdate()
                .orElseThrow(() -> new IllegalStateException("license_seats MAIN row not found"));

        OffsetDateTime now = OffsetDateTime.now();
        long active = sessionRepo.countActiveSessions(now);

        if (seats.getEnforceMode() == LicenseSeatsEntity.EnforceMode.HARD
                && active >= seats.getMaxConcurrentSeats()) {

            auditService.record("LOGIN_BLOCKED_SEAT_LIMIT", user, user, null,
                    "{\"maxSeats\":" + seats.getMaxConcurrentSeats() + ",\"activeSeats\":" + active + "}");
            throw new SeatLimitReachedException(seats.getMaxConcurrentSeats(), (int) active);
        }

        // 5) create session
        int refreshDays = cmd.rememberMe() ? refreshDaysRememberMe : refreshDaysDefault;

        String refreshToken = refreshTokenService.newToken();
        String refreshHash = refreshTokenService.hash(refreshToken);

        SessionEntity session = new SessionEntity();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setDeviceId(cmd.deviceId());
        session.setCreatedAt(now);
        session.setLastSeenAt(now);
        session.setExpiresAt(now.plusDays(refreshDays));
        session.setRevokedAt(null);
        session.setRefreshTokenHash(refreshHash);
        session.setPrevRefreshTokenHash(null);
        session.setIp(cmd.ip());
        session.setUserAgent(cmd.userAgent());

        sessionRepo.save(session);

        // 6) tokens
        UserAuthorizationContext authorization = userAuthorizationService.resolveForUser(user.getId());
        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getUsername(),
                session.getId().toString(),
                authorization.roles(),
                authorization.modules(),
                authorization.departments(),
                authorization.permissions()
        );

        auditService.record("LOGIN_SUCCESS", user, user, session,
                "{\"deviceId\":\"" + safe(cmd.deviceId()) + "\",\"ip\":\"" + safe(cmd.ip()) + "\"}");

        return new LoginResponse(
                accessToken,
                accessTokenMinutes * 60,
                refreshToken,
                refreshDays * 86400L,
                session.getId(),
                new LoginResponse.UserSummary(
                        user.getId(),
                        user.getUsername(),
                        user.getStatus().name(),
                        authorization.roles(),
                        authorization.modules(),
                        authorization.departments(),
                        authorization.permissions()
                )
        );
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "'");
    }
}
