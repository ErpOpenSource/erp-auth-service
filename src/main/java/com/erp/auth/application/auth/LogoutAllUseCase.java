package com.erp.auth.application.auth;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class LogoutAllUseCase {

    private final SessionJpaRepository sessionRepo;
    private final UserJpaRepository userRepo;
    private final AuditService auditService;

    public LogoutAllUseCase(
            SessionJpaRepository sessionRepo,
            UserJpaRepository userRepo,
            AuditService auditService
    ) {
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.auditService = auditService;
    }

    @Transactional
    public void execute(LogoutAllCommand cmd) {
        OffsetDateTime now = OffsetDateTime.now();
        int revoked = sessionRepo.revokeAllByUserId(cmd.userId(), now);

        UserEntity user = userRepo.findById(cmd.userId()).orElse(null);
        auditService.record("LOGOUT_ALL", user, user, null,
                "{\"userId\":\"" + cmd.userId() + "\",\"revokedCount\":" + revoked
                        + ",\"ip\":\"" + safe(cmd.ip()) + "\"}");
    }

    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("\"", "'");
    }
}
