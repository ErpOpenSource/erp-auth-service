package com.erp.auth.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_events",
       indexes = {
           @Index(name = "idx_audit_events_timestamp", columnList = "timestamp"),
           @Index(name = "idx_audit_events_type", columnList = "type")
       })
public class AuditEventEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UserEntity actorUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private UserEntity targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SessionEntity session;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String metadataJson; // simple; luego podemos mapear a JsonNode

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public UserEntity getActorUser() { return actorUser; }
    public void setActorUser(UserEntity actorUser) { this.actorUser = actorUser; }

    public UserEntity getTargetUser() { return targetUser; }
    public void setTargetUser(UserEntity targetUser) { this.targetUser = targetUser; }

    public SessionEntity getSession() { return session; }
    public void setSession(SessionEntity session) { this.session = session; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
