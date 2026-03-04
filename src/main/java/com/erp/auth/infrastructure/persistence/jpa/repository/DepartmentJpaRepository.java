package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, UUID> {
    Optional<DepartmentEntity> findByCode(String code);
    List<DepartmentEntity> findByCodeIn(Collection<String> codes);
}
