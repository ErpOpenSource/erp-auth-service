package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SessionJpaRepository extends JpaRepository<SessionEntity, UUID> {

    @Query("""
        select count(s)
        from SessionEntity s
        where s.revokedAt is null and s.expiresAt > :now
    """)
    long countActiveSessions(OffsetDateTime now);

    Optional<SessionEntity> findByRefreshTokenHash(String refreshTokenHash);
    Optional<SessionEntity> findByPrevRefreshTokenHash(String prevRefreshTokenHash);
}
