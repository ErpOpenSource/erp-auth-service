package com.erp.auth.interfaces.api;

import com.erp.auth.application.auth.UserProfileUseCase;
import com.erp.auth.infrastructure.security.JwtService;
import com.erp.auth.infrastructure.security.SecurityConfig;
import com.erp.auth.interfaces.api.dto.UserProfileResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@Import({SecurityConfig.class, SecurityAuthenticationEntryPoint.class, SecurityAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "auth.jwt.secret=change-this-super-secret-key-very-long-32-bytes-min",
        "auth.jwt.access-token-expiration-minutes=15"
})
class UserProfileControllerSecurityTest {

    private static final String SECRET = "change-this-super-secret-key-very-long-32-bytes-min";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileUseCase userProfileUseCase;

    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        verify(userProfileUseCase, never()).getProfile(any());
    }

    @Test
    void meWithValidTokenReturns200() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = tokenWithRoles(userId, List.of("USER"));

        when(userProfileUseCase.getProfile(eq(userId)))
                .thenReturn(new UserProfileResponse(
                        userId,
                        "operator",
                        "operator@erp.local",
                        "ACTIVE",
                        List.of("USER"),
                        List.of("SALES"),
                        List.of("HQ"),
                        List.of("sales.order.read")
                ));

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("operator"))
                .andExpect(jsonPath("$.email").value("operator@erp.local"));

        verify(userProfileUseCase).getProfile(eq(userId));
    }

    private static String tokenWithRoles(UUID userId, List<String> roles) {
        JwtService jwtService = new JwtService(SECRET, 15);
        return jwtService.generateAccessToken(
                userId.toString(),
                "operator",
                UUID.randomUUID().toString(),
                roles
        );
    }
}
