package com.infobee.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.UUID;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

/**
 * Stateless CSRF repository backed by the readable double-submit cookie.
 */
@Component
public class DoubleSubmitCsrfTokenRepository implements CsrfTokenRepository {
    private final String cookieName;
    private static final String HEADER_NAME = "X-CSRF-TOKEN";

    public DoubleSubmitCsrfTokenRepository(
        @Value("${app.security.csrf-cookie-name:INFOBEE_CSRF}") String cookieName
    ) {
        this.cookieName = cookieName;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        String value = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return new DefaultCsrfToken(HEADER_NAME, cookieName, value);
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        // Login creates the cookie with the configured Secure/SameSite attributes.
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return new DefaultCsrfToken(HEADER_NAME, cookieName, cookie.getValue());
            }
        }
        return null;
    }
}
