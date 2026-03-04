package com.erp.auth.interfaces.api.errors;

import com.erp.auth.infrastructure.observability.RequestIdFilter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .setValidator(validator)
                .build();
    }

    @Test
    void mapsSeatLimitTo409WithStandardBody() throws Exception {
        mockMvc.perform(get("/__test__/seat").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("SEAT_LIMIT_REACHED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.details.maxSeats").value(10))
                .andExpect(jsonPath("$.details.activeSeats").value(10));
    }

    @Test
    void mapsUnauthorizedAndForbiddenDomainErrors() throws Exception {
        mockMvc.perform(get("/__test__/invalid-refresh").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        mockMvc.perform(get("/__test__/revoked").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_REVOKED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        mockMvc.perform(get("/__test__/locked").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_LOCKED"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void mapsUnexpectedToInternalWithoutStacktraceInBody() throws Exception {
        mockMvc.perform(get("/__test__/unexpected").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Unexpected error."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void mapsValidationTo400WithDetails() throws Exception {
        mockMvc.perform(post("/__test__/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.fields.username").value("must not be blank"));
    }

    @RestController
    @RequestMapping("/__test__")
    static class ThrowingController {

        @GetMapping("/seat")
        String seat() {
            throw new SeatLimitReachedException(10, 10);
        }

        @GetMapping("/invalid-refresh")
        String invalidRefresh() {
            throw new InvalidRefreshTokenException();
        }

        @GetMapping("/revoked")
        String revoked() {
            throw new SessionRevokedException();
        }

        @GetMapping("/locked")
        String locked() {
            throw new UserLockedException();
        }

        @GetMapping("/unexpected")
        String unexpected() {
            throw new IllegalStateException("boom");
        }

        @PostMapping("/validation")
        String validation(@Valid @RequestBody ValidationRequest request) {
            return request.username();
        }
    }

    record ValidationRequest(@NotBlank String username) {
    }
}
