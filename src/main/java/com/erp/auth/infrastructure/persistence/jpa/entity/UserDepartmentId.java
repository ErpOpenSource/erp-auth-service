package com.erp.auth.infrastructure.persistence.jpa.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class UserDepartmentId implements Serializable {

    private UUID userId;
    private UUID departmentId;

    public UserDepartmentId() {
    }

    public UserDepartmentId(UUID userId, UUID departmentId) {
        this.userId = userId;
        this.departmentId = departmentId;
    }

    public UUID getUserId() { return userId; }
    public UUID getDepartmentId() { return departmentId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDepartmentId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(departmentId, that.departmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, departmentId);
    }
}
