package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.UserDepartmentEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserDepartmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface UserDepartmentJpaRepository extends JpaRepository<UserDepartmentEntity, UserDepartmentId> {

    @Query("""
        select ud.department.code
        from UserDepartmentEntity ud
        where ud.userId = :userId
        order by ud.department.code
    """)
    List<String> findDepartmentCodesByUserId(UUID userId);

    @Modifying
    @Query("delete from UserDepartmentEntity ud where ud.userId = :userId")
    int deleteByUserId(UUID userId);
}
