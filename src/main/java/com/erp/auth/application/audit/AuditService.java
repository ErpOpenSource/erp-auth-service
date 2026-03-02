package com.erp.auth.application.audit;

import com.erp.auth.infrastructure.persistence.jpa.entity.AuditEventEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.SessionEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.AuditEventJpaRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventJpaRepository auditRepo;

    public AuditService(AuditEventJpaRepository auditRepo) {
        this.auditRepo = auditRepo;
    }

    public void record(String type, UserEntity actor, UserEntity target, SessionEntity session, String metadataJson) {
        AuditEventEntity e = new AuditEventEntity();
        e.setId(UUID.randomUUID());
        e.setType(type);
        e.setActorUser(actor);
        e.setTargetUser(target);
        e.setSession(session);
        e.setTimestamp(OffsetDateTime.now());
        e.setTraceId(mdc("traceId")); // si no existe, queda null
        e.setMetadataJson(metadataJson == null ? "{}" : metadataJson);
        auditRepo.save(e);
    }

    private static String mdc(String key) {
        String v = MDC.get(key);
        return (v == null || v.isBlank()) ? null : v;
    }
}