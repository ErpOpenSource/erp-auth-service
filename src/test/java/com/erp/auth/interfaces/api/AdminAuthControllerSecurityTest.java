package com.erp.auth.interfaces.api;

import com.erp.auth.application.admin.AdminAuthorizationUseCase;
import com.erp.auth.application.admin.AdminLicensesUseCase;
import com.erp.auth.application.admin.AdminSessionsUseCase;
import com.erp.auth.application.admin.AdminUsersUseCase;
import com.erp.auth.infrastructure.security.JwtService;
import com.erp.auth.infrastructure.security.SecurityConfig;
import com.erp.auth.interfaces.api.dto.AdminUserResponse;
import com.erp.auth.interfaces.api.errors.SecurityAccessDeniedHandler;
import com.erp.auth.interfaces.api.errors.SecurityAuthenticationEntryPoint;
import com.erp.auth.interfaces.api.dto.ActiveSessionItem;
import com.erp.auth.interfaces.api.dto.ActiveSessionsResponse;
import com.erp.auth.interfaces.api.dto.SeatsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAuthController.class)
@Import({SecurityConfig.class, SecurityAuthenticationEntryPoint.class, SecurityAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "auth.jwt.secret=change-this-super-secret-key-very-long-32-bytes-min",
        "auth.jwt.access-token-expiration-minutes=15"
})
class AdminAuthControllerSecurityTest {

    private static final String SECRET = "change-this-super-secret-key-very-long-32-bytes-min";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminLicensesUseCase adminLicensesUseCase;
    @MockBean
    private AdminSessionsUseCase adminSessionsUseCase;
    @MockBean
    private AdminUsersUseCase adminUsersUseCase;
    @MockBean
    private AdminAuthorizationUseCase adminAuthorizationUseCase;

    @Test
    void adminEndpointReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/admin/licenses/seats"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        verify(adminLicensesUseCase, never()).getSeats(any(), any(), any());
    }

    @Test
    void adminEndpointReturns403ForNonAdminToken() throws Exception {
        String userToken = tokenWithRoles(List.of("USER"));

        mockMvc.perform(get("/admin/licenses/seats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        verify(adminLicensesUseCase, never()).getSeats(any(), any(), any());
    }

    @Test
    void adminEndpointAllowsAdminToken() throws Exception {
        String adminToken = tokenWithRoles(List.of("ADMIN"));
        when(adminLicensesUseCase.getSeats(any(), any(), any()))
                .thenReturn(new SeatsResponse(10, 3, "HARD"));

        mockMvc.perform(get("/admin/licenses/seats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminSessionsLegacyEndpointAllowsAdminToken() throws Exception {
        String adminToken = tokenWithRoles(List.of("ADMIN"));
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        when(adminSessionsUseCase.listActive(any(), any(), any()))
                .thenReturn(new ActiveSessionsResponse(List.of(
                        new ActiveSessionItem(
                                sessionId,
                                userId,
                                "admin",
                                "device-1",
                                "127.0.0.1",
                                "JUnit",
                                now.minusMinutes(5),
                                now.minusMinutes(1),
                                now.plusMinutes(10)
                        )
                )));

        mockMvc.perform(get("/admin/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(sessionId.toString()))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].username").value("admin"));
    }

    private static String tokenWithRoles(List<String> roles) {
        JwtService jwtService = new JwtService(SECRET, 15);
        return jwtService.generateAccessToken(
                UUID.randomUUID().toString(),
                "admin",
                UUID.randomUUID().toString(),
                roles
        );
    }
}
