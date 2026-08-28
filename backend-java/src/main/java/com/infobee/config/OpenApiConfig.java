package com.infobee.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI infobeeOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ATOM Platform Backend API")
                .description("Authentication, administration, ATOM, and CPSR request workflows.")
                .version("0.1.0")
                .contact(new Contact().name("ATOM backend team"))
                .license(new License().name("Proprietary")))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT access token. Use the value returned by /api/auth/login.")));
    }
}
