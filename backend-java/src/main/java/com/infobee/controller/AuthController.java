package com.infobee.controller;

import com.infobee.dto.LoginRequest;
import com.infobee.dto.SignupRequest;
import com.infobee.dto.AuthResponse;
import com.infobee.dto.UserResponse;
import com.infobee.model.ActivityLog;
import com.infobee.model.User;
import com.infobee.service.ActivityLogService;
import com.infobee.service.LoginAttemptService;
import com.infobee.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.infobee.service.TokenSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication", description = "Public account registration and cookie login")
public class AuthController {
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TokenSessionService tokenSessionService;
    private final LoginAttemptService loginAttemptService;
    private final ActivityLogService activityLogService;
    private final long jwtExpirationMs;
    private final String authCookieName;
    private final String csrfCookieName;
    private final boolean secureCookies;
    private final String sameSite;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthController(
        UserService userService,
        PasswordEncoder passwordEncoder,
        TokenSessionService tokenSessionService,
        LoginAttemptService loginAttemptService,
        ActivityLogService activityLogService,
        @Value("${app.jwt.expiration-ms}") long jwtExpirationMs,
        @Value("${app.security.auth-cookie-name:INFOBEE_AUTH}") String authCookieName,
        @Value("${app.security.csrf-cookie-name:INFOBEE_CSRF}") String csrfCookieName,
        @Value("${app.security.cookie-secure:false}") boolean secureCookies,
        @Value("${app.security.cookie-same-site:Lax}") String sameSite
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.tokenSessionService = tokenSessionService;
        this.loginAttemptService = loginAttemptService;
        this.activityLogService = activityLogService;
        this.jwtExpirationMs = jwtExpirationMs;
        this.authCookieName = authCookieName;
        this.csrfCookieName = csrfCookieName;
        this.secureCookies = secureCookies;
        this.sameSite = sameSite;
    }

    @PostMapping("/auth/login")
    @Operation(summary = "Log in", description = "Sets secure authentication cookies and returns a safe user profile. Public endpoint.")
    public AuthResponse login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse response
    ) {
        String username = request.username().trim();
        String clientIp = extractClientIp(httpRequest);

        String usernameKey = "user:" + username;
        String ipKey = "ip:" + clientIp;

        if (loginAttemptService.isBlocked(usernameKey) || loginAttemptService.isBlocked(ipKey)) {
            activityLogService.logWithoutActor(ActivityLog.Action.LOGIN_BLOCKED, null, null,
                username + " from " + clientIp, clientIp);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Too many failed attempts. Try again in 15 minutes.");
        }

        User user = userService.findByUsername(username)
            .orElseThrow(() -> {
                loginAttemptService.recordFailedAttempt(usernameKey);
                loginAttemptService.recordFailedAttempt(ipKey);
                return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
            });

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.recordFailedAttempt(usernameKey);
            loginAttemptService.recordFailedAttempt(ipKey);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        loginAttemptService.reset(usernameKey);
        loginAttemptService.reset(ipKey);

        String token = tokenSessionService.issue(user);
        addCookie(response, authCookieName, token, true, jwtExpirationMs);
        addCookie(response, csrfCookieName, csrfToken(), false, jwtExpirationMs);
        activityLogService.log(user, ActivityLog.Action.LOGIN, null, null, user.getUsername() + " logged in", clientIp);
        return new AuthResponse(UserResponse.from(user));
    }

    @org.springframework.web.bind.annotation.GetMapping("/auth/me")
    @Operation(summary = "Restore the current session", description = "Returns the authenticated user's safe profile.")
    public UserResponse me(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
            .map(UserResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "Log out", description = "Revokes the current JWT session.")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = cookieValue(request, authCookieName);
        String authorization = request.getHeader("Authorization");
        if (token == null && authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        tokenSessionService.revokeIfValid(token);
        clearCookie(response, authCookieName);
        clearCookie(response, csrfCookieName);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/signup")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sign up", description = "Creates an enabled USER account. Public endpoint; the role is never accepted from the client.")
    public UserResponse signup(@Valid @RequestBody SignupRequest request) {
        User user = userService.signup(request.username(), request.password(), request.fullName());
        activityLogService.log(user, ActivityLog.Action.SIGNUP, null, null, user.getUsername() + " signed up", null);
        return UserResponse.from(user);
    }

    private String csrfToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void addCookie(HttpServletResponse response, String name, String value, boolean httpOnly, long maxAgeMs) {
        long maxAgeSeconds = Math.max(1, (maxAgeMs + 999) / 1000);
        ResponseCookie cookie = ResponseCookie.from(name, value)
            .httpOnly(httpOnly)
            .secure(secureCookies)
            .sameSite(sameSite)
            .path("/")
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
            .httpOnly(name.equals(authCookieName))
            .secure(secureCookies)
            .sameSite(sameSite)
            .path("/")
            .maxAge(Duration.ZERO)
            .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
