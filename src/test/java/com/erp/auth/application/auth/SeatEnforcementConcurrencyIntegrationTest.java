package com.erp.auth.application.auth;

import com.erp.auth.infrastructure.persistence.jpa.entity.LicenseSeatsEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.RoleEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserEntity;
import com.erp.auth.infrastructure.persistence.jpa.entity.UserRoleEntity;
import com.erp.auth.infrastructure.persistence.jpa.repository.AuditEventJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.LicenseSeatsJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.RoleJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.SessionJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.erp.auth.infrastructure.persistence.jpa.repository.UserRoleJpaRepository;
import com.erp.auth.infrastructure.security.PasswordHasher;
import com.erp.auth.interfaces.api.errors.SeatLimitReachedException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class SeatEnforcementConcurrencyIntegrationTest {

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
    private LoginUseCase loginUseCase;
    @Autowired
    private PasswordHasher passwordHasher;
    @Autowired
    private AuditEventJpaRepository auditRepo;
    @Autowired
    private SessionJpaRepository sessionRepo;
    @Autowired
    private UserRoleJpaRepository userRoleRepo;
    @Autowired
    private RoleJpaRepository roleRepo;
    @Autowired
    private UserJpaRepository userRepo;
    @Autowired
    private LicenseSeatsJpaRepository seatsRepo;

    private String password;
    private UserEntity user1;
    private UserEntity user2;

    @BeforeEach
    void setUp() {
        auditRepo.deleteAll();
        sessionRepo.deleteAll();
        userRoleRepo.deleteAll();
        roleRepo.deleteAll();
        userRepo.deleteAll();
        seatsRepo.deleteAll();

        LicenseSeatsEntity seats = new LicenseSeatsEntity();
        seats.setId("MAIN");
        seats.setMaxConcurrentSeats(1);
        seats.setEnforceMode(LicenseSeatsEntity.EnforceMode.HARD);
        seats.setUpdatedAt(OffsetDateTime.now());
        seats.setVersion(0L);
        seatsRepo.save(seats);

        RoleEntity userRole = new RoleEntity();
        userRole.setId(UUID.randomUUID());
        userRole.setCode("USER");
        userRole.setName("Standard User");
        userRole.setCreatedAt(OffsetDateTime.now());
        userRole = roleRepo.save(userRole);

        password = "Password123!";
        user1 = saveUser("operator-1", "op1@erp.local", passwordHasher.hash(password));
        user2 = saveUser("operator-2", "op2@erp.local", passwordHasher.hash(password));

        userRoleRepo.save(link(user1.getId(), userRole.getId()));
        userRoleRepo.save(link(user2.getId(), userRole.getId()));
    }

    @Test
    void hardSeatLimitIsNotExceededUnderConcurrentLogins() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<Boolean>> tasks = List.of(
                loginTask(user1.getUsername(), "device-1", "10.0.0.11", ready, start),
                loginTask(user2.getUsername(), "device-2", "10.0.0.12", ready, start)
        );

        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> task : tasks) {
            futures.add(pool.submit(task));
        }

        assertTrue(ready.await(10, TimeUnit.SECONDS));
        start.countDown();

        int success = 0;
        int seatLimited = 0;
        for (Future<Boolean> future : futures) {
            try {
                if (future.get(30, TimeUnit.SECONDS)) {
                    success++;
                } else {
                    seatLimited++;
                }
            } catch (ExecutionException ex) {
                throw unwrap(ex);
            }
        }

        pool.shutdownNow();

        assertEquals(1, success);
        assertEquals(1, seatLimited);
        assertEquals(1, sessionRepo.countActiveSessions(OffsetDateTime.now()));
    }

    private Callable<Boolean> loginTask(
            String username,
            String deviceId,
            String ip,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return () -> {
            ready.countDown();
            start.await(10, TimeUnit.SECONDS);
            try {
                loginUseCase.execute(new LoginCommand(
                        username,
                        password,
                        deviceId,
                        false,
                        ip,
                        "JUnit"
                ));
                return true;
            } catch (SeatLimitReachedException ex) {
                return false;
            }
        };
    }

    private static RuntimeException unwrap(ExecutionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(cause);
    }

    private UserEntity saveUser(String username, String email, String passwordHash) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return userRepo.save(user);
    }

    private static UserRoleEntity link(UUID userId, UUID roleId) {
        UserRoleEntity link = new UserRoleEntity();
        link.setUserId(userId);
        link.setRoleId(roleId);
        link.setAssignedAt(OffsetDateTime.now());
        return link;
    }
}
