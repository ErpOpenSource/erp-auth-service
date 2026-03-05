package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.UserRoleEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, UserRoleId> {

    @Query("""
        select ur.role.code
        from UserRoleEntity ur
        where ur.userId = :userId
    """)
    List<String> findRoleCodesByUserId(UUID userId);

    @Modifying
    @Query("delete from UserRoleEntity ur where ur.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
