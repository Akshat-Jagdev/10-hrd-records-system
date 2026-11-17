package com.hrd.records.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI hrdOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HRD Records System API")
                        .description("Steel plant HR and production records platform. "
                                + "All data is synthetic sample data. "
                                + "Authenticate via /api/v1/auth/login, then use 'Authorize' with the JWT.")
                        .version("1.0.0")
                        .contact(new Contact().name("Akshat Jagdev").url("https://github.com/Akshat-Jagdev"))
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
