package com.infobee;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobee.service.LoginAttemptService;
import java.util.UUID;
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
class AdminIntegrationTest {
    private static final String AUTH_COOKIE = "ATOM_AUTH";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired LoginAttemptService loginAttemptService;

    @BeforeEach
    void resetLoginAttempts() {
        loginAttemptService.resetAll();
    }

    private String token(String username, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"%s"}
                    """.formatted(username, password)))
            .andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookie(AUTH_COOKIE).getValue();
    }

    @Test
    void adminListsAreProtectedAndReturnStablePagedDtos() throws Exception {
        String adminToken = token("admin", "admin123");
        String userToken = token("infobee", "infobee123");
        String marker = unique("Paged User");

        mvc.perform(post("/api/admin/users").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"strong-password","fullName":"%s","role":"USER"}
                    """.formatted(unique("paged-user"), marker)))
            .andExpect(status().isOk());

        mvc.perform(get("/api/admin/users?page=0&size=1&sort=username,asc&search=pAgEd")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].fullName").value(marker))
            .andExpect(jsonPath("$.content[0].password").doesNotExist())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));

        mvc.perform(get("/api/admin/departments").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").exists());
        mvc.perform(get("/api/admin/roles").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
        mvc.perform(get("/api/admin/menus").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());

        mvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminListParametersAreValidatedAndBounded() throws Exception {
        String adminToken = token("admin", "admin123");
        String auth = "Bearer " + adminToken;

        mvc.perform(get("/api/admin/users?size=101").header("Authorization", auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100));
        mvc.perform(get("/api/admin/users?page=-1").header("Authorization", auth))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/admin/users?size=0").header("Authorization", auth))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/admin/users?sort=secret,asc").header("Authorization", auth))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/admin/users?sort=id,sideways").header("Authorization", auth))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/admin/users?search=" + "x".repeat(101)).header("Authorization", auth))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void adminCrudPreservesConflictUpdateAndDeleteSemantics() throws Exception {
        String auth = "Bearer " + token("admin", "admin123");
        String department = unique("Department");
        String role = unique("Role");
        String username = unique("crud-user").toLowerCase();

        String departmentBody = mvc.perform(post("/api/admin/departments").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"%s"}
                    """.formatted(department)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long departmentId = mapper.readTree(departmentBody).get("id").asLong();
        mvc.perform(post("/api/admin/departments").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"%s"}
                    """.formatted(department.toLowerCase())))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/departments/" + departmentId).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"%s Updated"}
                    """.formatted(department)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(department + " Updated"));
        mvc.perform(delete("/api/admin/departments/" + departmentId).header("Authorization", auth))
            .andExpect(status().isOk());

        String roleBody = mvc.perform(post("/api/admin/roles").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"%s"}
                    """.formatted(role)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long roleId = mapper.readTree(roleBody).get("id").asLong();
        mvc.perform(post("/api/admin/roles").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"%s"}
                    """.formatted(role.toLowerCase())))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/roles/" + roleId).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"name":"%s2"}
                    """.formatted(role)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value((role + "2").toUpperCase()));
        mvc.perform(delete("/api/admin/roles/" + roleId).header("Authorization", auth))
            .andExpect(status().isOk());

        String menuBody = mvc.perform(post("/api/admin/menus").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"label":"Reports %s","path":"/reports/%s"}
                    """.formatted(role, role.toLowerCase())))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long menuId = mapper.readTree(menuBody).get("id").asLong();
        mvc.perform(put("/api/admin/menus/" + menuId).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"label":"Updated %s","path":"/updated/%s"}
                    """.formatted(role, role.toLowerCase())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.path").value("/updated/" + role.toLowerCase()));
        mvc.perform(delete("/api/admin/menus/" + menuId).header("Authorization", auth))
            .andExpect(status().isOk());

        String userBody = mvc.perform(post("/api/admin/users").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"username":"%s","password":"strong-password","fullName":"Original","role":"USER"}
                    """.formatted(username)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode user = mapper.readTree(userBody);
        long userId = user.get("id").asLong();
        mvc.perform(post("/api/admin/users").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"username":"%s","password":"strong-password","fullName":"Duplicate","role":"USER"}
                    """.formatted(username)))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/users/" + userId).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"username":"%s-updated","password":"strong-password","fullName":"Updated","role":"USER"}
                    """.formatted(username)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Updated"))
            .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(delete("/api/admin/users/" + userId).header("Authorization", auth))
            .andExpect(status().isOk());
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
