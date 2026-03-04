package com.erp.auth.application.admin;

import com.erp.auth.infrastructure.persistence.jpa.entity.LicenseSeatsEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.LicenseSeatsJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.interfaces.api.dto.LicenseStatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class LicenseService {

    private final LicenseSeatsJpaRepository seatsRepo;
    private final SessionJpaRepository sessionRepo;

    public LicenseService(
            LicenseSeatsJpaRepository seatsRepo,
            SessionJpaRepository sessionRepo
    ) {
        this.seatsRepo = seatsRepo;
        this.sessionRepo = sessionRepo;
    }

    @Transactional(readOnly = true)
    public LicenseStatusResponse getStatus() {
        LicenseSeatsEntity seats = seatsRepo.findById("MAIN")
                .orElseThrow(() -> new IllegalStateException("license_seats MAIN row not found"));

        int usedSeats = (int) sessionRepo.countActiveSessions(OffsetDateTime.now());
        return new LicenseStatusResponse(
                seats.getMaxConcurrentSeats(),
                usedSeats,
                seats.getEnforceMode().name()
        );
    }
}
