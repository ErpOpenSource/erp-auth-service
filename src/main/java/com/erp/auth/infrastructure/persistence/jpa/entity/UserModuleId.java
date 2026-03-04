package com.erp.auth.infrastructure.persistence.jpa.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserModuleId implements Serializable {

    private UUID userId;
    private UUID moduleId;

    public UserModuleId() {
    }

    public UserModuleId(UUID userId, UUID moduleId) {
        this.userId = userId;
        this.moduleId = moduleId;
    }

    public UUID getUserId() { return userId; }
    public UUID getModuleId() { return moduleId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserModuleId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(moduleId, that.moduleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, moduleId);
    }
}
