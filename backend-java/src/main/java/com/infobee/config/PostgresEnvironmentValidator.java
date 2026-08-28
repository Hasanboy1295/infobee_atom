package com.infobee.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("postgres")
public class PostgresEnvironmentValidator {
    public PostgresEnvironmentValidator(@Value("${spring.datasource.password:}") String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("DB_PASSWORD must be set for the postgres profile");
        }
    }
}
