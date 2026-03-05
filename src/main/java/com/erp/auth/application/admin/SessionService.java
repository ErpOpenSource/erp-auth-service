package com.erp.auth.application.admin;

import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.interfaces.api.dto.SessionSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SessionService {

    private final SessionJpaRepository sessionRepo;

    public SessionService(SessionJpaRepository sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> listActiveSessions() {
        return sessionRepo.findActiveSessions(OffsetDateTime.now()).stream()
                .map(session -> new SessionSummaryResponse(
                        session.getId().toString(),
                        session.getUser().getId().toString(),
                        session.getUser().getUsername(),
                        session.getIp(),
                        session.getUserAgent(),
                        session.getCreatedAt().toLocalDateTime(),
                        session.getLastSeenAt().toLocalDateTime(),
                        session.getExpiresAt().toLocalDateTime()
                ))
                .toList();
    }
}
