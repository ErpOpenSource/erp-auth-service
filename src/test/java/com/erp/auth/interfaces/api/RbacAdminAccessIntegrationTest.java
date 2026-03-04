package com.erp.auth.interfaces.api;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class RbacAdminAccessIntegrationTest {

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
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
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

    private String adminUsername;
    private String userUsername;
    private String password;

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
        seats.setMaxConcurrentSeats(10);
        seats.setEnforceMode(LicenseSeatsEntity.EnforceMode.HARD);
        seats.setUpdatedAt(OffsetDateTime.now());
        seats.setVersion(0L);
        seatsRepo.save(seats);

        RoleEntity adminRole = role("ADMIN", "Administrator");
        RoleEntity userRole = role("USER", "Standard User");
        roleRepo.save(adminRole);
        roleRepo.save(userRole);

        password = "Password123!";

        UserEntity admin = user("boss-admin", "boss@erp.local", passwordHasher.hash(password));
        UserEntity regular = user("operator-user", "operator@erp.local", passwordHasher.hash(password));
        admin = userRepo.save(admin);
        regular = userRepo.save(regular);
        adminUsername = admin.getUsername();
        userUsername = regular.getUsername();

        userRoleRepo.save(link(admin.getId(), adminRole.getId()));
        userRoleRepo.save(link(admin.getId(), userRole.getId()));
        userRoleRepo.save(link(regular.getId(), userRole.getId()));
    }

    @Test
    void adminRoleFromDbCanAccessAdminEndpoints() throws Exception {
        String token = loginAndGetAccessToken(adminUsername, password, "device-admin");

        mockMvc.perform(get("/admin/licenses/seats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxSeats").value(10));
    }

    @Test
    void userWithoutAdminRoleGets403() throws Exception {
        String token = loginAndGetAccessToken(userUsername, password, "device-user");

        mockMvc.perform(get("/admin/licenses/seats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    private String loginAndGetAccessToken(String username, String rawPassword, String deviceId) throws Exception {
        String body = """
                {
                  "username": "%s",
                  "password": "%s",
                  "deviceId": "%s"
                }
                """.formatted(username, rawPassword, deviceId);

        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private static RoleEntity role(String code, String name) {
        RoleEntity role = new RoleEntity();
        role.setId(UUID.randomUUID());
        role.setCode(code);
        role.setName(name);
        role.setCreatedAt(OffsetDateTime.now());
        return role;
    }

    private static UserEntity user(String username, String email, String passwordHash) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        user.setCreatedAt(OffsetDateTime.now());
        user.setUpdatedAt(OffsetDateTime.now());
        return user;
    }

    private static UserRoleEntity link(UUID userId, UUID roleId) {
        UserRoleEntity link = new UserRoleEntity();
        link.setUserId(userId);
        link.setRoleId(roleId);
        link.setAssignedAt(OffsetDateTime.now());
        return link;
    }
}
