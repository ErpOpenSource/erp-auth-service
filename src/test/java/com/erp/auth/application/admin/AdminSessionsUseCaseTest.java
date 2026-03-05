package com.erp.auth.application.admin;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.infrastructure.persistence.jpa.entity.SessionEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSessionsUseCaseTest {

    @Mock
    private SessionJpaRepository sessionRepo;
    @Mock
    private UserJpaRepository userRepo;
    @Mock
    private AuditService auditService;

    @Test
    void revokeRevokesAllActiveSessionsForSameUserAndDevice() {
        UUID actorId = UUID.randomUUID();
        UUID targetSessionId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        UserEntity actor = user(actorId, "admin");
        UserEntity targetUser = user(targetUserId, "operator");

        SessionEntity targetSession = new SessionEntity();
        targetSession.setId(targetSessionId);
        targetSession.setUser(targetUser);
        targetSession.setDeviceId("device-1");

        AdminSessionsUseCase useCase = new AdminSessionsUseCase(sessionRepo, userRepo, auditService);

        when(userRepo.findById(actorId)).thenReturn(Optional.of(actor));
        when(sessionRepo.findById(targetSessionId)).thenReturn(Optional.of(targetSession));
        when(sessionRepo.revokeByUserIdAndDeviceId(eq(targetUserId), eq("device-1"), any(OffsetDateTime.class)))
                .thenReturn(2);

        useCase.revoke(actorId, targetSessionId, "127.0.0.1", "JUnit");

        verify(sessionRepo).revokeByUserIdAndDeviceId(eq(targetUserId), eq("device-1"), any(OffsetDateTime.class));
        verify(auditService).record(eq("ADMIN_SESSION_REVOKE"), eq(actor), eq(targetUser), eq(targetSession), contains("\"revokedCount\":2"));
    }

    @Test
    void revokeAllExceptUsesCurrentSessionId() {
        UUID actorId = UUID.randomUUID();
        UUID currentSessionId = UUID.randomUUID();
        UserEntity actor = user(actorId, "admin");

        AdminSessionsUseCase useCase = new AdminSessionsUseCase(sessionRepo, userRepo, auditService);

        when(userRepo.findById(actorId)).thenReturn(Optional.of(actor));
        when(sessionRepo.revokeAllExceptSessionId(eq(currentSessionId), any(OffsetDateTime.class)))
                .thenReturn(5);

        useCase.revokeAllExcept(actorId, currentSessionId, "127.0.0.1", "JUnit");

        verify(sessionRepo).revokeAllExceptSessionId(eq(currentSessionId), any(OffsetDateTime.class));
        verify(auditService).record(eq("ADMIN_SESSION_REVOKE_ALL"), eq(actor), isNull(), isNull(), contains("\"revokedCount\":5"));
    }

    private static UserEntity user(UUID id, String username) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
