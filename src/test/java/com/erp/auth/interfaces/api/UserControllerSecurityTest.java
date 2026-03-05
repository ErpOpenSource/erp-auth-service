package com.erp.auth.interfaces.api;

import com.erp.auth.application.user.UserService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, SecurityAuthenticationEntryPoint.class, SecurityAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "auth.jwt.secret=change-this-super-secret-key-very-long-32-bytes-min",
        "auth.jwt.access-token-expiration-minutes=15"
})
class UserControllerSecurityTest {

    private static final String SECRET = "change-this-super-secret-key-very-long-32-bytes-min";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void meEndpointReturns401WithoutToken() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getProfile(anyString());
    }

    @Test
    void meEndpointUsesJwtSubjectAsUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        JwtService jwtService = new JwtService(SECRET, 15);
        String token = jwtService.generateAccessToken(
                userId.toString(),
                "operator",
                UUID.randomUUID().toString()
        );

        when(userService.getProfile(userId.toString()))
                .thenReturn(new UserProfileResponse(
                        userId,
                        "operator",
                        "operator@erp.local",
                        "ACTIVE",
                        List.of("USER"),
                        List.of("AUTH"),
                        List.of(),
                        List.of("AUTH:READ")
                ));

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("operator"));

        verify(userService).getProfile(userId.toString());
    }
}
