package com.erp.auth.application.auth;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.infrastructure.persistence.jpa.entity.SessionEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.interfaces.api.errors.SessionRevokedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class LogoutUseCase {

    private final SessionJpaRepository sessionRepo;
    private final AuditService auditService;

    public LogoutUseCase(SessionJpaRepository sessionRepo, AuditService auditService) {
        this.sessionRepo = sessionRepo;
        this.auditService = auditService;
    }

    @Transactional
    public void execute(LogoutCommand cmd) {
        SessionEntity session = sessionRepo.findById(cmd.sessionId())
                .orElseThrow(SessionRevokedException::new);

        OffsetDateTime now = OffsetDateTime.now();
        int updated = sessionRepo.revokeById(session.getId(), now);

        auditService.record("LOGOUT", session.getUser(), session.getUser(), session,
                "{\"sessionId\":\"" + session.getId() + "\",\"revoked\":" + (updated > 0)
                        + ",\"ip\":\"" + safe(cmd.ip()) + "\"}");
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "'");
    }
}
