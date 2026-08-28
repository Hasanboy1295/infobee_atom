package com.infobee;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SecurityConfigurationTest {
    @Test
    void postgresMigrationRemovesHistoricalDemoUsers() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration-postgres/V7__remove_demo_users.sql"),
            StandardCharsets.UTF_8);
        assertThat(migration).contains("DELETE FROM users");
        assertThat(migration).contains("'admin'");
        assertThat(migration).contains("'infobee'");
    }

    @Test
    void productionPropertiesDoNotEnableH2OrPublicDocs() throws Exception {
        String properties = Files.readString(Path.of("src/main/resources/application-postgres.properties"),
            StandardCharsets.UTF_8);
        assertThat(properties).contains("spring.h2.console.enabled=false");
        assertThat(properties).contains("app.security.api-docs-public=${API_DOCS_PUBLIC:false}");
        assertThat(properties).contains("app.jwt.secret=${JWT_SECRET:}");
    }
}
