package com.asms.springasms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes the JWT bearer scheme in springdoc Swagger UI (the "Authorize" button),
 * mirroring the .NET backend's Scalar/Swagger auth support. All endpoints except
 * {@code POST /api/auth/login} require {@code Authorization: Bearer <token>}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI asmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Assignment & Submission Management System")
                        .version("1.0")
                        .description("Spring Boot port of the OnnoRokomBackend API"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
