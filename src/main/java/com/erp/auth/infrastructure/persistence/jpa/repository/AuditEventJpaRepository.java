package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, UUID> {
}
