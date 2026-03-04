package com.erp.auth.application.admin;

import com.erp.auth.application.audit.AuditService;
import com.erp.auth.infrastructure.persistence.jpa.entity.LicenseSeatsEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.LicenseSeatsJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.erp.auth.interfaces.api.dto.SeatsResponse;
import com.erp.auth.interfaces.api.dto.UpdateSeatsRequest;
import com.erp.auth.interfaces.api.errors.ApiException;
import com.erp.auth.interfaces.api.errors.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminLicensesUseCase {

    private final LicenseSeatsJpaRepository seatsRepo;
    private final SessionJpaRepository sessionRepo;
    private final UserJpaRepository userRepo;
    private final AuditService auditService;

    public AdminLicensesUseCase(
            LicenseSeatsJpaRepository seatsRepo,
            SessionJpaRepository sessionRepo,
            UserJpaRepository userRepo,
            AuditService auditService
    ) {
        this.seatsRepo = seatsRepo;
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public SeatsResponse getSeats(UUID actorUserId, String ip, String userAgent) {
        LicenseSeatsEntity seats = seatsRepo.findById("MAIN")
                .orElseThrow(() -> new IllegalStateException("license_seats MAIN row not found"));

        long activeSeats = sessionRepo.countActiveSessions(OffsetDateTime.now());
        SeatsResponse response = new SeatsResponse(
                seats.getMaxConcurrentSeats(),
                (int) activeSeats,
                seats.getEnforceMode().name()
        );

        UserEntity actor = userRepo.findById(actorUserId).orElse(null);
        auditService.record("ADMIN_SEATS_VIEW", actor, null, null,
                "{\"maxSeats\":" + response.maxSeats()
                        + ",\"activeSeats\":" + response.activeSeats()
                        + ",\"enforceMode\":\"" + response.enforceMode() + "\""
                        + ",\"ip\":\"" + safe(ip) + "\""
                        + ",\"userAgent\":\"" + safe(userAgent) + "\"}");
        return response;
    }

    @Transactional
    public SeatsResponse updateSeats(UUID actorUserId, UpdateSeatsRequest request, String ip, String userAgent) {
        LicenseSeatsEntity seats = seatsRepo.lockMainForUpdate()
                .orElseThrow(() -> new IllegalStateException("license_seats MAIN row not found"));

        LicenseSeatsEntity.EnforceMode mode;
        try {
            mode = LicenseSeatsEntity.EnforceMode.valueOf(request.enforceMode().trim().toUpperCase());
        } catch (Exception ex) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Invalid enforceMode. Use HARD or SOFT.",
                    Map.of("enforceMode", request.enforceMode())
            );
        }

        int previousMax = seats.getMaxConcurrentSeats();
        String previousMode = seats.getEnforceMode().name();

        seats.setMaxConcurrentSeats(request.maxSeats());
        seats.setEnforceMode(mode);
        seats.setUpdatedAt(OffsetDateTime.now());
        seatsRepo.save(seats);

        long activeSeats = sessionRepo.countActiveSessions(OffsetDateTime.now());
        SeatsResponse response = new SeatsResponse(
                seats.getMaxConcurrentSeats(),
                (int) activeSeats,
                seats.getEnforceMode().name()
        );

        UserEntity actor = userRepo.findById(actorUserId).orElse(null);
        auditService.record("ADMIN_SEATS_UPDATE", actor, null, null,
                "{\"previousMaxSeats\":" + previousMax
                        + ",\"newMaxSeats\":" + response.maxSeats()
                        + ",\"previousEnforceMode\":\"" + previousMode + "\""
                        + ",\"newEnforceMode\":\"" + response.enforceMode() + "\""
                        + ",\"ip\":\"" + safe(ip) + "\""
                        + ",\"userAgent\":\"" + safe(userAgent) + "\"}");
        return response;
    }

    private static String safe(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\"", "'");
    }
}
