package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, UUID> {

    interface PermissionClaimProjection {
        String getCode();
        String getModuleCode();
    }

    Optional<PermissionEntity> findByCode(String code);
    List<PermissionEntity> findByCodeIn(Collection<String> codes);

    @Query("""
        select distinct p.code as code, m.code as moduleCode
        from UserRoleEntity ur
        join RolePermissionEntity rp on ur.roleId = rp.roleId
        join PermissionEntity p on p.id = rp.permissionId
        left join ModuleEntity m on p.moduleId = m.id
        where ur.userId = :userId
        order by p.code
    """)
    List<PermissionClaimProjection> findPermissionClaimsByUserId(UUID userId);
}
