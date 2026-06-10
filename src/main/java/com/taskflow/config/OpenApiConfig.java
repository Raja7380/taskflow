package com.taskflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OPENAPI / SWAGGER CONFIGURATION
 *
 * Defines:
 * 1. API metadata (title, version, description) shown in Swagger UI
 * 2. "bearerAuth" security scheme — registers the JWT auth mechanism
 *    so the "Authorize" button appears in Swagger UI
 *
 * Without this, @SecurityRequirement(name = "bearerAuth") in controllers
 * references a scheme that doesn't exist — the lock icons won't show.
 *
 * After login, paste your access token in Swagger's "Authorize" dialog:
 *   Value: Bearer eyJhbGciOiJIUzI1NiJ9...
 *   (include the word "Bearer " before the token)
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "TaskFlow API",
                version = "2.0",
                description = "Project Management System — Sessions 1 & 2 (Auth + Projects & Tasks)"
        )
)
@SecurityScheme(
        name = "bearerAuth",                    // This name matches @SecurityRequirement(name = "bearerAuth")
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",                   // Display hint in Swagger UI
        in = SecuritySchemeIn.HEADER            // Token goes in Authorization header
)
public class OpenApiConfig {
    // No beans needed — annotations on the class are enough
}
