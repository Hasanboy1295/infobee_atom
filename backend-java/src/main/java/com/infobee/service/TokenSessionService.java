package com.infobee.service;

import com.infobee.model.TokenSession;
import com.infobee.model.User;
import com.infobee.repository.TokenSessionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenSessionService {
    private final TokenSessionRepository repository;
    private final JwtService jwtService;

    public TokenSessionService(TokenSessionRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
    }

    @Transactional
    public String issue(User user) {
        String token = jwtService.generateToken(user);
        Claims claims = jwtService.parseToken(token);
        repository.save(new TokenSession(
            claims.getId(),
            claims.getSubject(),
            claims.getExpiration().toInstant()
        ));
        return token;
    }

    @Transactional(readOnly = true)
    public boolean isActive(String jti, String username) {
        if (jti == null || username == null) {
            return false;
        }
        return repository.findByJtiAndUsername(jti, username)
            .filter(session -> session.getRevokedAt() == null)
            .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
            .isPresent();
    }

    @Transactional
    public void revoke(String token) {
        Claims claims = jwtService.parseToken(token);
        repository.findByJtiAndUsername(claims.getId(), claims.getSubject())
            .ifPresent(session -> {
                session.revoke();
                repository.save(session);
            });
    }

    @Transactional
    public void revokeIfValid(String token) {
            if (token == null || token.isBlank()) {
                return;
            }
            try {
                revoke(token);
            } catch (JwtException | IllegalArgumentException ignored) {
                // Logout is idempotent and must not disclose token parsing details.
            }
    }
}
