package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.RolePermissionEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {

    @Query("select rp.permission.code from RolePermissionEntity rp where rp.roleId = :roleId")
    List<String> findPermissionCodesByRoleId(@Param("roleId") UUID roleId);

    @Modifying
    @Query("delete from RolePermissionEntity rp where rp.roleId = :roleId")
    int deleteByRoleId(UUID roleId);
}
