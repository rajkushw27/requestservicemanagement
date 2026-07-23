package com.rajani.makerchecker.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui.html, raw spec at /v3/api-docs — the whole point being that
 *  a system integrating with this service doesn't need to read the Java source to find
 *  the contract. */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI makerCheckerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Counterfoil — maker-checker API")
                        .description("""
                                Generic four-eyes approval service. Submit a change, get told who must sign it,
                                track decisions, get a webhook when it's decided. Two ways to authenticate:
                                Authorization: Bearer <token> from POST /api/v1/auth/login for the demo UI, or
                                X-Api-Key + X-Acting-As for a calling system with no human in the loop.
                                """)
                        .version("1.0.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("Human login token from POST /api/v1/auth/login"))
                        .addSecuritySchemes("apiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Api-Key")
                                .description("System-to-system key. Must be paired with an X-Acting-As header naming the employee id.")));
    }
}
