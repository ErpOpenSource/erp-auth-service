package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.LicenseSeatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface LicenseSeatsJpaRepository extends JpaRepository<LicenseSeatsEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LicenseSeatsEntity l where l.id = 'MAIN'")
    Optional<LicenseSeatsEntity> lockMainForUpdate();
}