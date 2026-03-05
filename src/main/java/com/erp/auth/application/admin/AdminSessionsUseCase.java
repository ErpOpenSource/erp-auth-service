package com.erp.auth.application.admin;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.infrastructure.persistence.jpa.entity.SessionEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.erp.auth.interfaces.api.dto.ActiveSessionItem;
import com.erp.auth.interfaces.api.dto.ActiveSessionsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AdminSessionsUseCase {

    private final SessionJpaRepository sessionRepo;
    private final UserJpaRepository userRepo;
    private final AuditService auditService;

    public AdminSessionsUseCase(
            SessionJpaRepository sessionRepo,
            UserJpaRepository userRepo,
            AuditService auditService
    ) {
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ActiveSessionsResponse listActive(UUID actorUserId, String ip, String userAgent) {
        OffsetDateTime now = OffsetDateTime.now();
        List<ActiveSessionItem> items = sessionRepo.findActiveSessions(now).stream()
                .map(s -> new ActiveSessionItem(
                        s.getId(),
                        s.getUser().getId(),
                        s.getUser().getUsername(),
                        s.getDeviceId(),
                        s.getIp(),
                        s.getUserAgent(),
                        s.getCreatedAt(),
                        s.getLastSeenAt(),
                        s.getExpiresAt()
                ))
                .toList();

        UserEntity actor = userRepo.findById(actorUserId).orElse(null);
        auditService.record("ADMIN_SESSIONS_LIST", actor, null, null,
                "{\"activeCount\":" + items.size()
                        + ",\"ip\":\"" + safe(ip) + "\""
                        + ",\"userAgent\":\"" + safe(userAgent) + "\"}");
        return new ActiveSessionsResponse(items);
    }

    @Transactional
    public void revoke(UUID actorUserId, UUID sessionId, String ip, String userAgent) {
        OffsetDateTime now = OffsetDateTime.now();
        SessionEntity session = sessionRepo.findById(sessionId).orElse(null);
        UserEntity actor = userRepo.findById(actorUserId).orElse(null);

        if (session == null) {
            auditService.record("ADMIN_SESSION_REVOKE", actor, null, null,
                    "{\"sessionId\":\"" + sessionId + "\",\"result\":\"NOT_FOUND\"}");
            return;
        }

        int updated = sessionRepo.revokeByUserIdAndDeviceId(
                session.getUser().getId(),
                session.getDeviceId(),
                now
        );
        auditService.record("ADMIN_SESSION_REVOKE", actor, session.getUser(), session,
                "{\"sessionId\":\"" + sessionId + "\",\"revoked\":" + (updated > 0)
                        + ",\"revokedCount\":" + updated
                        + ",\"deviceId\":\"" + safe(session.getDeviceId()) + "\""
                        + ",\"ip\":\"" + safe(ip) + "\""
                        + ",\"userAgent\":\"" + safe(userAgent) + "\"}");
    }

    @Transactional
    public void revokeAllExcept(UUID actorUserId, UUID keepSessionId, String ip, String userAgent) {
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity actor = userRepo.findById(actorUserId).orElse(null);

        int updated = sessionRepo.revokeAllExceptSessionId(keepSessionId, now);

        auditService.record("ADMIN_SESSION_REVOKE_ALL", actor, null, null,
                "{\"keepSessionId\":\"" + keepSessionId + "\""
                        + ",\"revokedCount\":" + updated
                        + ",\"ip\":\"" + safe(ip) + "\""
                        + ",\"userAgent\":\"" + safe(userAgent) + "\"}");
    }

    @Transactional
    public void revokeAllExcept(UUID actorUserId, UUID keepSessionId, String ip, String userAgent) {
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity actor = userRepo.findById(actorUserId).orElse(null);

        int revoked;
        if (keepSessionId == null) {
            revoked = sessionRepo.revokeAll(now);
        } else {
            revoked = sessionRepo.revokeAllExcept(keepSessionId, now);
        }

        auditService.record("ADMIN_SESSIONS_REVOKE_ALL", actor, null, null,
                "{\"keepSessionId\":\"" + (keepSessionId == null ? "" : keepSessionId) + "\""
                        + ",\"revokedCount\":" + revoked
                        + ",\"ip\":\"" + safe(ip) + "\""
                        + ",\"userAgent\":\"" + safe(userAgent) + "\"}");
    }

    private static String safe(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\"", "'");
    }
}
