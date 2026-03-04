package com.erp.auth.application.auth;

import com.erp.auth.infrastructure.persistence.jpa.entity.SessionEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserRoleEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.RoleEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserRoleJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.RoleJpaRepository;
import com.erp.auth.infrastructure.security.RefreshTokenService;
import com.erp.auth.interfaces.api.dto.RefreshResponse;
import com.erp.auth.interfaces.api.errors.RefreshReuseDetectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class RefreshUseCaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("erp_auth_test")
            .withUsername("erp")
            .withPassword("erp");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("management.otlp.metrics.export.enabled", () -> "false");
        registry.add("auth.jwt.secret", () -> "change-this-super-secret-key-very-long-32-bytes-min");
        registry.add("auth.jwt.access-token-expiration-minutes", () -> "15");
        registry.add("auth.refresh.expiration-days-default", () -> "7");
        registry.add("auth.refresh.expiration-days-remember-me", () -> "30");
        registry.add("auth.refresh.pepper", () -> "integration-pepper");
    }

    @Autowired
    private RefreshUseCase refreshUseCase;
    @Autowired
    private SessionJpaRepository sessionRepo;
    @Autowired
    private UserJpaRepository userRepo;
    @Autowired
    private UserRoleJpaRepository userRoleRepo;
    @Autowired
    private RoleJpaRepository roleRepo;
    @Autowired
    private RefreshTokenService refreshTokenService;

    private UserEntity user;
    private SessionEntity session;
    private String originalRefreshToken;

    @BeforeEach
    void setUp() {
        sessionRepo.deleteAll();
        userRoleRepo.deleteAll();
        roleRepo.deleteAll();
        userRepo.deleteAll();

        user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("integration-admin");
        user.setEmail(null);
        user.setPasswordHash("dummy");
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        user = userRepo.save(user);

        RoleEntity adminRole = new RoleEntity();
        adminRole.setId(UUID.randomUUID());
        adminRole.setCode("ADMIN");
        adminRole.setName("Administrator");
        adminRole.setCreatedAt(OffsetDateTime.now());
        adminRole = roleRepo.save(adminRole);

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(adminRole.getId());
        userRole.setAssignedAt(OffsetDateTime.now());
        userRoleRepo.save(userRole);

        originalRefreshToken = "refresh-original-token";
        session = new SessionEntity();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        session.setDeviceId("device-1");
        session.setCreatedAt(OffsetDateTime.now().minusMinutes(5));
        session.setLastSeenAt(OffsetDateTime.now().minusMinutes(5));
        session.setExpiresAt(OffsetDateTime.now().plusDays(1));
        session.setRevokedAt(null);
        session.setRefreshTokenHash(refreshTokenService.hash(originalRefreshToken));
        session.setPrevRefreshTokenHash(null);
        session.setIp("127.0.0.1");
        session.setUserAgent("JUnit");
        session = sessionRepo.save(session);
    }

    @Test
    void refreshRotationAndReuseDetection() {
        RefreshResponse response = refreshUseCase.execute(new RefreshCommand(
                originalRefreshToken,
                "device-1",
                "10.10.0.1",
                "JUnit"
        ));

        SessionEntity rotated = sessionRepo.findById(session.getId()).orElseThrow();
        assertEquals(refreshTokenService.hash(originalRefreshToken), rotated.getPrevRefreshTokenHash());
        assertNotEquals(refreshTokenService.hash(originalRefreshToken), rotated.getRefreshTokenHash());
        assertEquals(refreshTokenService.hash(response.refreshToken()), rotated.getRefreshTokenHash());
        assertNotNull(response.accessToken());
        assertFalse(response.accessToken().isBlank());

        assertThrows(RefreshReuseDetectedException.class, () -> refreshUseCase.execute(new RefreshCommand(
                originalRefreshToken,
                "device-1",
                "10.10.0.2",
                "JUnit"
        )));

        SessionEntity revoked = sessionRepo.findById(session.getId()).orElseThrow();
        assertNotNull(revoked.getRevokedAt());
    }
}
