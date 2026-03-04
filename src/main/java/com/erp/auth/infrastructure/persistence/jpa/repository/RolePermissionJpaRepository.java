package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.RolePermissionEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {

    @Modifying
    @Query("delete from RolePermissionEntity rp where rp.roleId = :roleId")
    int deleteByRoleId(UUID roleId);
}
