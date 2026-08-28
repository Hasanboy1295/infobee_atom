package com.infobee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobee.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class BackendIntegrationTest {
    private static final String AUTH_COOKIE = "ATOM_AUTH";
    private static final String CSRF_COOKIE = "ATOM_CSRF";

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
    void browserLoginUsesCookiesAndRequiresDoubleSubmitCsrf() throws Exception {
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"infobee","password":"infobee123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.user.username").value("infobee"))
            .andReturn();
        Cookie auth = login.getResponse().getCookie(AUTH_COOKIE);
        Cookie csrf = login.getResponse().getCookie(CSRF_COOKIE);
        assertThat(auth).isNotNull();
        assertThat(csrf).isNotNull();
        assertThat(login.getResponse().getHeaders("Set-Cookie"))
            .anyMatch(value -> value.contains(AUTH_COOKIE + "=")
                && value.contains("HttpOnly") && value.contains("SameSite=Lax")
                && value.contains("Path=/") && value.contains("Max-Age=3600"));
        assertThat(login.getResponse().getHeaders("Set-Cookie"))
            .anyMatch(value -> value.contains(CSRF_COOKIE + "=") && !value.contains("HttpOnly"));

        mvc.perform(post("/api/atom-requests").cookie(auth, csrf)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"csrf test\",\"description\":\"csrf test\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/atom-requests").cookie(auth, csrf)
                .header("X-CSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"csrf accepted\",\"description\":\"csrf accepted\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/auth/me").cookie(auth, csrf))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("infobee"));
        mvc.perform(post("/api/auth/logout").cookie(auth, csrf)
                .header("X-CSRF-TOKEN", csrf.getValue()))
            .andExpect(status().isNoContent())
            .andExpect(header().string("Set-Cookie",
                org.hamcrest.Matchers.containsString(AUTH_COOKIE + "=;")));
        mvc.perform(get("/api/auth/me").cookie(auth, csrf))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void signupRejectsDuplicateAndNeverReturnsPassword() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"new-user","password":"strong-password","fullName":"New User"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"new-user","password":"strong-password","fullName":"New User"}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void requestWorkflowEnforcesOwnershipAndAdminApproval() throws Exception {
        String userToken = token("infobee", "infobee123");
        String adminToken = token("admin", "admin123");
        String response = mvc.perform(post("/api/atom-requests").header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Atom request","description":"Manual review input"}
                    """))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(response).get("id").asLong();

        mvc.perform(post("/api/atom-requests/%d/submit".formatted(id)).header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(post("/api/atom-requests/%d/approve".formatted(id)).header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/atom-requests/%d/approve".formatted(id)).header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void unauthenticatedDomainAccessIsJson401() throws Exception {
        mvc.perform(get("/api/atom-requests"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void disabledUsersCannotLoginOrUsePreviouslyIssuedTokens() throws Exception {
        String signupResponse = mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"disabled-user","password":"strong-password","fullName":"Disabled User"}
                    """))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long disabledUserId = mapper.readTree(signupResponse).get("id").asLong();
        String userToken = token("disabled-user", "strong-password");
        String adminToken = token("admin", "admin123");

        mvc.perform(patch("/api/admin/users/" + disabledUserId + "/enabled").param("enabled", "false")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"disabled-user","password":"strong-password"}
                    """))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/atom-requests").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void logoutRevokesThePersistentTokenSession() throws Exception {
        String userToken = token("infobee", "infobee123");

        mvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/atom-requests")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void malformedJsonAndPageableInputsReturnApiErrors() throws Exception {
        String userToken = token("infobee", "infobee123");
        mvc.perform(post("/api/atom-requests").header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.validationErrors").exists());
        mvc.perform(get("/api/atom-requests?page=abc").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/atom-requests?sort=notAField").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void corsIsLimitedToConfiguredDevelopmentOrigin() throws Exception {
        mvc.perform(options("/api/atom-requests")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        mvc.perform(options("/api/atom-requests")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }

    @Test
    void paginationIsBoundedAndOwnershipIsEnforced() throws Exception {
        String ownerToken = token("infobee", "infobee123");
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"other-owner","password":"strong-password","fullName":"Other Owner"}
                    """))
            .andExpect(status().isCreated());
        String otherToken = token("other-owner", "strong-password");
        String response = mvc.perform(post("/api/atom-requests").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Private request","description":"Private description"}
                    """))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(response).get("id").asLong();

        mvc.perform(get("/api/atom-requests?size=101").header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100));
        mvc.perform(get("/api/atom-requests/" + id).header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/api/atom-requests/" + id + "/comments").header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/atom-requests/" + id + "/history").header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void dashboardStatsReturnsValidStructure() throws Exception {
        String userToken = token("infobee", "infobee123");
        mvc.perform(get("/api/stats/dashboard").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUsers").isNumber())
            .andExpect(jsonPath("$.activeUsers").isNumber())
            .andExpect(jsonPath("$.totalDepartments").isNumber())
            .andExpect(jsonPath("$.totalRoles").isNumber())
            .andExpect(jsonPath("$.totalAtomRequests").isNumber())
            .andExpect(jsonPath("$.totalCpsrRequests").isNumber())
            .andExpect(jsonPath("$.atomByStatus").exists())
            .andExpect(jsonPath("$.cpsrByStatus").exists())
            .andExpect(jsonPath("$.totalComments").isNumber())
            .andExpect(jsonPath("$.totalHistoryEntries").isNumber());
    }

    @Test
    void dashboardStatsRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/stats/dashboard"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidAndCriticalTransitionsReturnExpectedStatuses() throws Exception {
        String ownerToken = token("infobee", "infobee123");
        String adminToken = token("admin", "admin123");
        String response = mvc.perform(post("/api/cpsr-requests").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Transition request","description":"Transition description"}
                    """))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(response).get("id").asLong();

        mvc.perform(post("/api/cpsr-requests/%d/not-a-transition".formatted(id))
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/cpsr-requests/%d/review".formatted(id))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/cpsr-requests/%d/cancel".formatted(id))
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

        String fullPathResponse = mvc.perform(post("/api/cpsr-requests").header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Full path request","description":"Full path description"}
                    """))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long fullPathId = mapper.readTree(fullPathResponse).get("id").asLong();
        mvc.perform(post("/api/cpsr-requests/%d/submit".formatted(fullPathId))
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(post("/api/cpsr-requests/%d/review".formatted(fullPathId))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
        mvc.perform(post("/api/cpsr-requests/%d/reject".formatted(fullPathId))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        mvc.perform(post("/api/cpsr-requests/%d/submit".formatted(fullPathId))
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"));
        mvc.perform(post("/api/cpsr-requests/%d/approve".formatted(fullPathId))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void listFiltersByStatusAndSearchText() throws Exception {
        String userToken = token("infobee", "infobee123");
        mvc.perform(post("/api/atom-requests").header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Filter Alpha\",\"description\":\"unique-alpha-text\"}"))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/atom-requests").header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Filter Beta\",\"description\":\"unique-beta-text\"}"))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/atom-requests?search=unique-alpha").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Filter Alpha"));

        mvc.perform(get("/api/atom-requests?search=nonexistent-xyz").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(0));

        var draftResult = mvc.perform(get("/api/atom-requests?status=DRAFT").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andReturn();
        int draftCount = mapper.readTree(draftResult.getResponse().getContentAsString()).get("totalElements").asInt();
        assertThat(draftCount).isGreaterThanOrEqualTo(2);

        mvc.perform(get("/api/atom-requests?priority=URGENT").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void listFiltersByPriorityAndCombinesWithStatus() throws Exception {
        String userToken = token("infobee", "infobee123");

        String resp1 = mvc.perform(post("/api/atom-requests").header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Priority Test\",\"description\":\"desc\",\"priority\":\"HIGH\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id1 = mapper.readTree(resp1).get("id").asLong();

        mvc.perform(post("/api/atom-requests/%d/submit".formatted(id1))
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/atom-requests?priority=HIGH&status=DRAFT").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0));

        mvc.perform(get("/api/atom-requests?priority=HIGH&status=SUBMITTED").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(id1));

        mvc.perform(get("/api/atom-requests?createdFrom=2020-01-01T00:00:00Z&createdTo=2099-12-31T23:59:59Z")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void activityLogsRequireAdminAndRecordLoginAndRequestEvents() throws Exception {
        String userToken = token("infobee", "infobee123");
        String adminToken = token("admin", "admin123");

        mvc.perform(get("/api/activity-logs").header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());

        mvc.perform(get("/api/activity-logs").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").isNumber());

        var logsResult = mvc.perform(get("/api/activity-logs").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk()).andReturn();
        JsonNode logs = mapper.readTree(logsResult.getResponse().getContentAsString());
        assertThat(logs.get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
        JsonNode firstLog = logs.get("content").get(0);
        assertThat(firstLog.has("id")).isTrue();
        assertThat(firstLog.has("action")).isTrue();
        assertThat(firstLog.has("createdAt")).isTrue();
    }

    @Test
    void activityLogsCanBeFilteredByAction() throws Exception {
        String userToken = token("infobee", "infobee123");
        String adminToken = token("admin", "admin123");

        mvc.perform(post("/api/atom-requests").header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Audit Test Request\",\"description\":\"audit test\"}"))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/activity-logs?action=REQUEST_CREATED").header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].action").value("REQUEST_CREATED"));
    }
}
