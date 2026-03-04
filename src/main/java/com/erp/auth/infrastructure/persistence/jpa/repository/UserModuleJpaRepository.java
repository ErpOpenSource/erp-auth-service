package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.UserModuleEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserModuleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserModuleJpaRepository extends JpaRepository<UserModuleEntity, UserModuleId> {

    @Query("""
        select um.module.code
        from UserModuleEntity um
        where um.userId = :userId
        order by um.module.code
    """)
    List<String> findModuleCodesByUserId(UUID userId);

    @Modifying
    @Query("delete from UserModuleEntity um where um.userId = :userId")
    int deleteByUserId(UUID userId);
}
