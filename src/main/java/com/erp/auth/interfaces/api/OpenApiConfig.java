package com.erp.auth.interfaces.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI authOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ERP Auth Service")
                        .version("v1")
                        .description("Authentication, sessions, licensing seats, and audit."));
    }
}