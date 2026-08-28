package com.infobee.config;

import com.infobee.model.User;
import com.infobee.service.JwtService;
import com.infobee.service.UserService;
import com.infobee.service.TokenSessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserService userService;
    private final TokenSessionService tokenSessionService;
    private final String authCookieName;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        UserService userService,
        TokenSessionService tokenSessionService,
        @Value("${app.security.auth-cookie-name:INFOBEE_AUTH}") String authCookieName
    ) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.tokenSessionService = tokenSessionService;
        this.authCookieName = authCookieName;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String token = cookieValue(request, authCookieName);
        String header = request.getHeader("Authorization");
        if (token == null && header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }
        if (token != null
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwtService.parseToken(token);
                User user = userService.findByUsername(claims.getSubject()).orElse(null);
                if (user != null && user.isEnabled()
                    && tokenSessionService.isActive(claims.getId(), claims.getSubject())) {
                    String role = user.getRole();
                    var authentication = new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                // Invalid or expired tokens remain unauthenticated.
            }
        }
        filterChain.doFilter(request, response);
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
