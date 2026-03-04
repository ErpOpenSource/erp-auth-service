package com.erp.auth.interfaces.api;

import com.erp.auth.application.auth.LoginUseCase;
import com.erp.auth.application.auth.LogoutAllUseCase;
import com.erp.auth.application.auth.LogoutUseCase;
import com.erp.auth.application.auth.RefreshUseCase;
import com.erp.auth.infrastructure.security.JwtService;
import com.erp.auth.infrastructure.security.SecurityConfig;
import com.erp.auth.interfaces.api.errors.SecurityAccessDeniedHandler;
import com.erp.auth.interfaces.api.errors.SecurityAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, SecurityAuthenticationEntryPoint.class, SecurityAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "auth.jwt.secret=change-this-super-secret-key-very-long-32-bytes-min",
        "auth.jwt.access-token-expiration-minutes=15"
})
class AuthControllerSecurityTest {

    private static final String SECRET = "change-this-super-secret-key-very-long-32-bytes-min";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginUseCase loginUseCase;
    @MockBean
    private RefreshUseCase refreshUseCase;
    @MockBean
    private LogoutUseCase logoutUseCase;
    @MockBean
    private LogoutAllUseCase logoutAllUseCase;

    @Test
    void logoutAllWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/logout/all"))
                .andExpect(status().isUnauthorized());

        verify(logoutAllUseCase, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void logoutAllWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(post("/logout/all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized());

        verify(logoutAllUseCase, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void logoutAllWithValidTokenUsesSubAsUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtService jwtService = new JwtService(SECRET, 15);
        String token = jwtService.generateAccessToken(
                userId.toString(),
                "admin",
                UUID.randomUUID().toString()
        );

        mockMvc.perform(post("/logout/all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("User-Agent", "JUnit"))
                .andExpect(status().isNoContent());

        verify(logoutAllUseCase).execute(argThat(cmd -> cmd.userId().equals(userId)));
    }
}
