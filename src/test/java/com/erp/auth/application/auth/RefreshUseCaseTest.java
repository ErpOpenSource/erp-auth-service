package com.erp.auth.application.auth;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.infrastructure.persistence.jpa.entity.SessionEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.infrastructure.security.JwtService;
import com.erp.auth.infrastructure.security.RefreshTokenService;
import com.erp.auth.interfaces.api.dto.RefreshResponse;
import com.erp.auth.interfaces.api.errors.InvalidRefreshTokenException;
import com.erp.auth.interfaces.api.errors.RefreshReuseDetectedException;
import com.erp.auth.interfaces.api.errors.SessionRevokedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshUseCaseTest {

    @Mock
    private SessionJpaRepository sessionRepo;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuditService auditService;
    @Mock
    private UserAuthorizationService userAuthorizationService;

    private RefreshUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RefreshUseCase(sessionRepo, refreshTokenService, jwtService, auditService, userAuthorizationService, 15L);
    }

    @Test
    void executeRotatesRefreshAndReturnsNewTokens() {
        SessionEntity session = activeSession("device-1");
        RefreshCommand cmd = new RefreshCommand("old-refresh", "device-1", "10.0.0.10", "JUnit");

        when(refreshTokenService.hash("old-refresh")).thenReturn("incoming-hash");
        when(sessionRepo.findByPrevRefreshTokenHash("incoming-hash")).thenReturn(Optional.empty());
        when(sessionRepo.findByRefreshTokenHash("incoming-hash")).thenReturn(Optional.of(session));
        when(userAuthorizationService.resolveForUser(session.getUser().getId())).thenReturn(
                new UserAuthorizationContext(
                        List.of("ADMIN"),
                        List.of("SALES"),
                        List.of("HQ"),
                        List.of("sales.order.read")
                )
        );
        when(refreshTokenService.newToken()).thenReturn("new-refresh");
        when(refreshTokenService.hash("new-refresh")).thenReturn("new-hash");
        when(jwtService.generateAccessToken(
                session.getUser().getId().toString(),
                session.getUser().getUsername(),
                session.getId().toString(),
                List.of("ADMIN"),
                List.of("SALES"),
                List.of("HQ"),
                List.of("sales.order.read")
        )).thenReturn("access-token");

        RefreshResponse response = useCase.execute(cmd);

        assertEquals("access-token", response.accessToken());
        assertEquals("new-refresh", response.refreshToken());
        assertEquals(15L * 60, response.accessTokenExpiresInSeconds());
        assertEquals(session.getId(), response.sessionId());
        assertEquals("old-hash", session.getPrevRefreshTokenHash());
        assertEquals("new-hash", session.getRefreshTokenHash());
        assertEquals("10.0.0.10", session.getIp());
        assertEquals("JUnit", session.getUserAgent());
        assertNotNull(session.getLastSeenAt());

        verify(sessionRepo).save(session);
        verify(auditService).record(eq("REFRESH_SUCCESS"), eq(session.getUser()), eq(session.getUser()), eq(session), contains("\"deviceId\":\"device-1\""));
    }

    @Test
    void executeDetectsReuseAndRevokesSession() {
        SessionEntity reusedSession = activeSession("device-1");
        RefreshCommand cmd = new RefreshCommand("reused-token", "device-1", "10.0.0.11", "JUnit");

        when(refreshTokenService.hash("reused-token")).thenReturn("reused-hash");
        when(sessionRepo.findByPrevRefreshTokenHash("reused-hash")).thenReturn(Optional.of(reusedSession));

        assertThrows(RefreshReuseDetectedException.class, () -> useCase.execute(cmd));

        assertNotNull(reusedSession.getRevokedAt());
        verify(sessionRepo).save(reusedSession);
        verify(sessionRepo, never()).findByRefreshTokenHash(anyString());
        verify(auditService).record(eq("REFRESH_REUSE_DETECTED"), eq(reusedSession.getUser()), eq(reusedSession.getUser()), eq(reusedSession), contains("\"ip\":\"10.0.0.11\""));
    }

    @Test
    void executeRejectsUnknownRefreshToken() {
        RefreshCommand cmd = new RefreshCommand("unknown-token", "device-1", "10.0.0.12", "JUnit");

        when(refreshTokenService.hash("unknown-token")).thenReturn("unknown-hash");
        when(sessionRepo.findByPrevRefreshTokenHash("unknown-hash")).thenReturn(Optional.empty());
        when(sessionRepo.findByRefreshTokenHash("unknown-hash")).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> useCase.execute(cmd));

        verify(auditService).record(eq("REFRESH_FAILED"), isNull(), isNull(), isNull(), contains("INVALID_REFRESH_TOKEN"));
        verify(sessionRepo, never()).save(any(SessionEntity.class));
    }

    @Test
    void executeRejectsRevokedOrExpiredSession() {
        SessionEntity revoked = activeSession("device-1");
        revoked.setRevokedAt(OffsetDateTime.now().minusMinutes(1));
        RefreshCommand cmd = new RefreshCommand("current-token", "device-1", "10.0.0.13", "JUnit");

        when(refreshTokenService.hash("current-token")).thenReturn("current-hash");
        when(sessionRepo.findByPrevRefreshTokenHash("current-hash")).thenReturn(Optional.empty());
        when(sessionRepo.findByRefreshTokenHash("current-hash")).thenReturn(Optional.of(revoked));

        assertThrows(SessionRevokedException.class, () -> useCase.execute(cmd));

        verify(auditService).record(eq("REFRESH_FAILED"), eq(revoked.getUser()), eq(revoked.getUser()), eq(revoked), contains("SESSION_REVOKED_OR_EXPIRED"));
        verify(sessionRepo, never()).save(any(SessionEntity.class));
    }

    @Test
    void executeRejectsDeviceMismatch() {
        SessionEntity session = activeSession("device-A");
        RefreshCommand cmd = new RefreshCommand("current-token", "device-B", "10.0.0.14", "JUnit");

        when(refreshTokenService.hash("current-token")).thenReturn("current-hash");
        when(sessionRepo.findByPrevRefreshTokenHash("current-hash")).thenReturn(Optional.empty());
        when(sessionRepo.findByRefreshTokenHash("current-hash")).thenReturn(Optional.of(session));

        assertThrows(InvalidRefreshTokenException.class, () -> useCase.execute(cmd));

        verify(auditService).record(eq("REFRESH_FAILED"), eq(session.getUser()), eq(session.getUser()), eq(session), contains("DEVICE_MISMATCH"));
        verify(sessionRepo, never()).save(any(SessionEntity.class));
    }

    private static SessionEntity activeSession(String deviceId) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("admin");
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        user.setPasswordHash("hash");
        user.setCreatedAt(OffsetDateTime.now().minusDays(1));
        user.setUpdatedAt(OffsetDateTime.now().minusDays(1));

        SessionEntity session = new SessionEntity();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setDeviceId(deviceId);
        session.setCreatedAt(OffsetDateTime.now().minusHours(1));
        session.setLastSeenAt(OffsetDateTime.now().minusMinutes(5));
        session.setExpiresAt(OffsetDateTime.now().plusDays(7));
        session.setRefreshTokenHash("old-hash");
        session.setPrevRefreshTokenHash(null);
        session.setRevokedAt(null);
        session.setIp("127.0.0.1");
        session.setUserAgent("JUnit");
        return session;
    }
}
