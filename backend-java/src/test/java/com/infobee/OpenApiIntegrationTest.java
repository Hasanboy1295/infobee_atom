package com.infobee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobee.service.LoginAttemptService;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class OpenApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired OpenAPI openAPI;
    @Autowired LoginAttemptService loginAttemptService;

    @BeforeEach
    void resetLoginAttempts() {
        loginAttemptService.resetAll();
    }

    @Test
    void contextLoadsWithOpenApiConfiguration() {
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("ATOM Platform Backend API");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
    }

    @Test
    void publicDocsDescribeSecurityWithoutCredentialFields() throws Exception {
        String body = mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        JsonNode document = mapper.readTree(body);
        assertThat(document.path("components").path("securitySchemes")
            .path("bearerAuth").path("scheme").asText()).isEqualTo("bearer");
        assertThat(body).doesNotContain("\"password\"");
        assertThat(body).doesNotContain("\"secret\"");
    }

    @Test
    void legacyUserDirectoryIsAdminOnly() throws Exception {
        String token = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"infobee","password":"infobee123"}
                    """))
            .andExpect(status().isOk())
            .andReturn().getResponse().getCookie("ATOM_AUTH").getValue();

        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden());
    }
}
