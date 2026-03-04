package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleJpaRepository extends JpaRepository<ModuleEntity, UUID> {
    Optional<ModuleEntity> findByCode(String code);
    List<ModuleEntity> findByCodeIn(Collection<String> codes);
}
