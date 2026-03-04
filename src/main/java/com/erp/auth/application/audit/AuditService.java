package com.erp.auth.application.audit;

import com.erp.auth.infrastructure.persistence.jpa.entity.AuditEventEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.SessionEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.AuditEventJpaRepository;
import com.erp.auth.infrastructure.observability.RequestIdFilter;
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
        e.setRequestId(mdc("requestId"));
        e.setMetadataJson(enrichMetadata(metadataJson));
        auditRepo.save(e);
    }

    private static String enrichMetadata(String metadataJson) {
        String base = metadataJson == null || metadataJson.isBlank() ? "{}" : metadataJson.trim();
        if (!base.startsWith("{") || !base.endsWith("}")) {
            return base;
        }

        String ip = mdc(RequestIdFilter.MDC_CLIENT_IP);
        String userAgent = mdc(RequestIdFilter.MDC_USER_AGENT);

        boolean hasIp = base.contains("\"ip\"");
        boolean hasUserAgent = base.contains("\"userAgent\"");

        if ((ip == null || hasIp) && (userAgent == null || hasUserAgent)) {
            return base;
        }

        String body = base.substring(1, base.length() - 1).trim();
        StringBuilder out = new StringBuilder("{");
        boolean hasContent = !body.isEmpty();

        if (hasContent) {
            out.append(body);
        }
        if (ip != null && !hasIp) {
            if (hasContent) {
                out.append(",");
            }
            out.append("\"ip\":\"").append(escape(ip)).append("\"");
            hasContent = true;
        }
        if (userAgent != null && !hasUserAgent) {
            if (hasContent) {
                out.append(",");
            }
            out.append("\"userAgent\":\"").append(escape(userAgent)).append("\"");
        }
        out.append("}");
        return out.toString();
    }

    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static String mdc(String key) {
        String v = MDC.get(key);
        return (v == null || v.isBlank()) ? null : v;
    }
}
