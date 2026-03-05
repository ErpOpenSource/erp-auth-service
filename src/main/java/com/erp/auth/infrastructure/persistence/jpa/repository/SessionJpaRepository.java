package com.erp.auth.infrastructure.persistence.jpa.repository;

import com.erp.auth.infrastructure.persistence.jpa.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
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

    @Query("""
        select s
        from SessionEntity s
        join fetch s.user u
        where s.revokedAt is null and s.expiresAt > :now
        order by s.createdAt desc
    """)
    List<SessionEntity> findActiveSessions(OffsetDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SessionEntity s
        set s.revokedAt = :revokedAt
        where s.id = :sessionId and s.revokedAt is null
    """)
    int revokeById(UUID sessionId, OffsetDateTime revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SessionEntity s
        set s.revokedAt = :revokedAt
        where s.user.id = :userId and s.revokedAt is null
    """)
    int revokeAllByUserId(UUID userId, OffsetDateTime revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SessionEntity s
        set s.revokedAt = :revokedAt
        where s.revokedAt is null
    """)
    int revokeAll(OffsetDateTime revokedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update SessionEntity s
        set s.revokedAt = :revokedAt
        where s.revokedAt is null and s.id <> :keepSessionId
    """)
    int revokeAllExcept(UUID keepSessionId, OffsetDateTime revokedAt);
}
