package com.infobee.config;

import com.infobee.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infobee.error.ApiError;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CsrfCookieFilter csrfCookieFilter;
    private final ObjectMapper objectMapper;
    private final String allowedOrigins;
    private final boolean h2ConsoleEnabled;
    private final boolean apiDocsPublic;
    private final String authCookieName;
    private final DoubleSubmitCsrfTokenRepository csrfTokenRepository;

    public SecurityConfig(
        JwtAuthenticationFilter jwtAuthenticationFilter,
        CsrfCookieFilter csrfCookieFilter,
        ObjectMapper objectMapper,
        @Value("${app.cors.allowed-origins:http://localhost:3000}") String allowedOrigins,
        @Value("${app.security.h2-console-enabled:false}") boolean h2ConsoleEnabled,
        @Value("${app.security.api-docs-public:false}") boolean apiDocsPublic,
        @Value("${app.security.auth-cookie-name:INFOBEE_AUTH}") String authCookieName,
        DoubleSubmitCsrfTokenRepository csrfTokenRepository,
        @Value("${spring.profiles.active:}") String activeProfile
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.csrfCookieFilter = csrfCookieFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigins = allowedOrigins;
        boolean localProfile = Arrays.stream(activeProfile.split(","))
            .map(String::trim)
            .anyMatch("local"::equals);
        this.h2ConsoleEnabled = localProfile && h2ConsoleEnabled;
        this.apiDocsPublic = localProfile && apiDocsPublic;
        this.authCookieName = authCookieName;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .ignoringRequestMatchers(request ->
                    !hasCookie((HttpServletRequest) request, authCookieName)
                        || Boolean.TRUE.equals(request.getAttribute(CsrfCookieFilter.VALIDATED_ATTRIBUTE))))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/signup", "/api/auth/logout", "/api/health").permitAll()
                .requestMatchers("/h2-console/**").access((authentication, context) ->
                    new org.springframework.security.authorization.AuthorizationDecision(h2ConsoleEnabled))
                .requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                    .access((authentication, context) -> new org.springframework.security.authorization.AuthorizationDecision(
                        apiDocsPublic || (authentication.get() != null
                            && authentication.get().getAuthorities().stream()
                                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")))))
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                .accessDeniedHandler(jsonAccessDeniedHandler()))
            .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
            .addFilterBefore(csrfCookieFilter, org.springframework.security.web.csrf.CsrfFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private boolean hasCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return false;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return true;
        }
        return false;
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
            .map(user -> org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .disabled(!user.isEnabled())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (request, response, exception) -> {
            writeError(response, 401, "Unauthorized", "Authentication is required");
        };
    }

    private AccessDeniedHandler jsonAccessDeniedHandler() {
        return (request, response, exception) -> {
            writeError(response, 403, "Forbidden", "Access is denied");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isBlank())
            .toList();
        boolean wildcard = origins.contains("*");
        CorsConfiguration configuration = new CorsConfiguration();
        if (wildcard) {
            configuration.setAllowedOriginPatterns(List.of("*"));
            configuration.setAllowCredentials(false);
        } else {
            configuration.setAllowedOrigins(origins);
            configuration.setAllowCredentials(true);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-CSRF-TOKEN"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private void writeError(HttpServletResponse response, int status, String error, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), new ApiError(Instant.now(), status, error, message, java.util.Map.of()));
    }
}
