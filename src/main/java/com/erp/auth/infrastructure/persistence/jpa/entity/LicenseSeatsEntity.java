package com.erp.auth.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "license_seats")
public class LicenseSeatsEntity {

    @Id
    @Column(name = "id", nullable = false, length = 32)
    private String id; // "MAIN"

    @Column(name = "max_concurrent_seats", nullable = false)
    private int maxConcurrentSeats;

    @Enumerated(EnumType.STRING)
    @Column(name = "enforce_mode", nullable = false, length = 16)
    private EnforceMode enforceMode;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public enum EnforceMode { HARD, SOFT }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getMaxConcurrentSeats() { return maxConcurrentSeats; }
    public void setMaxConcurrentSeats(int maxConcurrentSeats) { this.maxConcurrentSeats = maxConcurrentSeats; }

    public EnforceMode getEnforceMode() { return enforceMode; }
    public void setEnforceMode(EnforceMode enforceMode) { this.enforceMode = enforceMode; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}