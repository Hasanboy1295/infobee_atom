package com.infobee.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobee.error.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Double-submit CSRF protection for browser sessions. Bearer-only API clients
 * remain compatible because the check is applied only when the auth cookie is present.
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {
    public static final String VALIDATED_ATTRIBUTE = CsrfCookieFilter.class.getName() + ".validated";
    private static final String CSRF_HEADER = "X-CSRF-TOKEN";
    private final ObjectMapper objectMapper;
    private final String authCookieName;
    private final String csrfCookieName;

    public CsrfCookieFilter(
        ObjectMapper objectMapper,
        @Value("${app.security.auth-cookie-name:INFOBEE_AUTH}") String authCookieName,
        @Value("${app.security.csrf-cookie-name:INFOBEE_CSRF}") String csrfCookieName
    ) {
        this.objectMapper = objectMapper;
        this.authCookieName = authCookieName;
        this.csrfCookieName = csrfCookieName;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        boolean authCookiePresent = hasCookie(request, authCookieName);
        if (requiresProtection(request) && authCookiePresent) {
            String cookieToken = cookieValue(request, csrfCookieName);
            String headerToken = request.getHeader(CSRF_HEADER);
            if (cookieToken == null || headerToken == null || !Objects.equals(cookieToken, headerToken)) {
                writeError(response);
                return;
            }
        }
        if (authCookiePresent) request.setAttribute(VALIDATED_ATTRIBUTE, Boolean.TRUE);
        filterChain.doFilter(request, response);
    }

    private boolean requiresProtection(HttpServletRequest request) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
            || "OPTIONS".equalsIgnoreCase(method) || "TRACE".equalsIgnoreCase(method)) {
            return false;
        }
        String path = request.getRequestURI();
        return !"/api/auth/login".equals(path) && !"/api/auth/signup".equals(path);
    }

    private boolean hasCookie(HttpServletRequest request, String name) {
        return cookieValue(request, name) != null;
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private void writeError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), new ApiError(
            Instant.now(), 403, "Forbidden", "CSRF token is missing or invalid", java.util.Map.of()));
    }
}
