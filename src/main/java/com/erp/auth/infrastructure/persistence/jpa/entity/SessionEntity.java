package com.erp.auth.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions",
       indexes = {
           @Index(name = "idx_sessions_user_id", columnList = "user_id"),
           @Index(name = "idx_sessions_refresh_hash", columnList = "refresh_token_hash")
       })
public class SessionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "refresh_token_hash", nullable = false, columnDefinition = "text")
    private String refreshTokenHash;

    @Column(name = "prev_refresh_token_hash", columnDefinition = "text")
    private String prevRefreshTokenHash;

    @Column(name = "ip", columnDefinition = "inet")
    @ColumnTransformer(write = "?::inet")
    private String ip;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(OffsetDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }

    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(OffsetDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getRefreshTokenHash() { return refreshTokenHash; }
    public void setRefreshTokenHash(String refreshTokenHash) { this.refreshTokenHash = refreshTokenHash; }

    public String getPrevRefreshTokenHash() { return prevRefreshTokenHash; }
    public void setPrevRefreshTokenHash(String prevRefreshTokenHash) { this.prevRefreshTokenHash = prevRefreshTokenHash; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
