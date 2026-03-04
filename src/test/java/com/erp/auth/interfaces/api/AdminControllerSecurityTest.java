package com.erp.auth.interfaces.api;

import com.erp.auth.application.admin.LicenseService;
import com.erp.auth.application.admin.SessionService;
import com.erp.auth.application.user.UserService;
import com.erp.auth.infrastructure.security.JwtService;
import com.erp.auth.infrastructure.security.SecurityConfig;
import com.erp.auth.interfaces.api.dto.LicenseStatusResponse;
import com.erp.auth.interfaces.api.errors.SecurityAccessDeniedHandler;
import com.erp.auth.interfaces.api.errors.SecurityAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, SecurityAuthenticationEntryPoint.class, SecurityAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "auth.jwt.secret=change-this-super-secret-key-very-long-32-bytes-min",
        "auth.jwt.access-token-expiration-minutes=15"
})
class AdminControllerSecurityTest {

    private static final String SECRET = "change-this-super-secret-key-very-long-32-bytes-min";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    @MockBean
    private SessionService sessionService;
    @MockBean
    private LicenseService licenseService;

    @Test
    void adminEndpointReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/admin/license"))
                .andExpect(status().isUnauthorized());

        verify(licenseService, never()).getStatus();
    }

    @Test
    void adminEndpointReturns403ForNonAdminToken() throws Exception {
        String userToken = tokenWithRoles(List.of("USER"));

        mockMvc.perform(get("/admin/license")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden());

        verify(licenseService, never()).getStatus();
    }

    @Test
    void adminEndpointAllowsAdminToken() throws Exception {
        String adminToken = tokenWithRoles(List.of("ADMIN"));
        when(licenseService.getStatus())
                .thenReturn(new LicenseStatusResponse(10, 3, "HARD"));

        mockMvc.perform(get("/admin/license")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxConcurrentSeats").value(10))
                .andExpect(jsonPath("$.usedSeats").value(3))
                .andExpect(jsonPath("$.enforceMode").value("HARD"));
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
