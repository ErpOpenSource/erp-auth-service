package com.erp.auth.application.auth;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.infrastructure.persistence.jpa.entity.SessionEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.infrastructure.security.JwtService;
import com.erp.auth.infrastructure.security.RefreshTokenService;
import com.erp.auth.interfaces.api.dto.RefreshResponse;
import com.erp.auth.interfaces.api.errors.InvalidRefreshTokenException;
import com.erp.auth.interfaces.api.errors.RefreshReuseDetectedException;
import com.erp.auth.interfaces.api.errors.SessionRevokedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class RefreshUseCase {

    private final SessionJpaRepository sessionRepo;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final long accessTokenMinutes;

    public RefreshUseCase(
            SessionJpaRepository sessionRepo,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            AuditService auditService,
            @Value("${auth.jwt.access-token-expiration-minutes}") long accessTokenMinutes
    ) {
        this.sessionRepo = sessionRepo;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    @Transactional
    public RefreshResponse execute(RefreshCommand cmd) {

        OffsetDateTime now = OffsetDateTime.now();

        String incomingHash = refreshTokenService.hash(cmd.refreshToken());

        // 1) Reuse detection: token coincide con el PREV de una sesión => robo/reuse
        var reused = sessionRepo.findByPrevRefreshTokenHash(incomingHash);
        if (reused.isPresent()) {
            SessionEntity s = reused.get();

            // Revocamos la sesión inmediatamente
            if (s.getRevokedAt() == null) {
                s.setRevokedAt(now);
                sessionRepo.save(s);
            }

            auditService.record("REFRESH_REUSE_DETECTED", s.getUser(), s.getUser(), s,
                    "{\"deviceId\":\"" + safe(cmd.deviceId()) + "\",\"ip\":\"" + safe(cmd.ip()) + "\"}");

            throw new RefreshReuseDetectedException();
        }

        // 2) Token normal: debe coincidir con refresh_token_hash
        SessionEntity session = sessionRepo.findByRefreshTokenHash(incomingHash)
                .orElseThrow(() -> {
                    auditService.record("REFRESH_FAILED", null, null, null, "{\"reason\":\"INVALID_REFRESH_TOKEN\"}");
                    return new InvalidRefreshTokenException();
                });

        // 3) Validación sesión
        if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(now)) {
            auditService.record("REFRESH_FAILED", session.getUser(), session.getUser(), session,
                    "{\"reason\":\"SESSION_REVOKED_OR_EXPIRED\"}");
            throw new SessionRevokedException();
        }

        // (Opcional) Validar deviceId coincide con el de la sesión
        // Si quieres forzarlo enterprise:
        if (!session.getDeviceId().equals(cmd.deviceId())) {
            auditService.record("REFRESH_FAILED", session.getUser(), session.getUser(), session,
                    "{\"reason\":\"DEVICE_MISMATCH\"}");
            throw new InvalidRefreshTokenException();
        }

        // 4) Rotación: prev <- current, current <- new
        String newRefreshToken = refreshTokenService.newToken();
        String newHash = refreshTokenService.hash(newRefreshToken);

        session.setPrevRefreshTokenHash(session.getRefreshTokenHash());
        session.setRefreshTokenHash(newHash);

        session.setLastSeenAt(now);
        session.setIp(cmd.ip());
        session.setUserAgent(cmd.userAgent());

        sessionRepo.save(session);

        // 5) Nuevo access token (sid y sub se mantienen)
        String accessToken = jwtService.generateAccessToken(
                session.getUser().getId().toString(),
                session.getUser().getUsername(),
                session.getId().toString()
        );

        auditService.record("REFRESH_SUCCESS", session.getUser(), session.getUser(), session,
                "{\"deviceId\":\"" + safe(cmd.deviceId()) + "\"}");

        // TTL refresh restante (no extendemos expiresAt). Calculamos seconds restantes.
        long refreshRemainingSeconds = Math.max(0,
                session.getExpiresAt().toEpochSecond() - now.toEpochSecond()
        );

        return new RefreshResponse(
                accessToken,
                accessTokenMinutes * 60,
                newRefreshToken,
                refreshRemainingSeconds,
                session.getId()
        );
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "'");
    }
}