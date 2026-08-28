package com.infobee.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.infobee.model.User;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private final JwtService jwtService = new JwtService(
        "test-secret-key-that-is-at-least-32-characters-long",
        60_000
    );

    @Test
    void generatesAndParsesUserToken() {
        User user = new User("admin", "ignored", "System Administrator", "ADMIN");

        String token = jwtService.generateToken(user);

        assertEquals("admin", jwtService.parseToken(token).getSubject());
        assertEquals("ADMIN", jwtService.parseToken(token).get("role", String.class));
        assertEquals(36, jwtService.parseToken(token).getId().length());
    }

    @Test
    void rejectsTamperedToken() {
        User user = new User("admin", "ignored", "System Administrator", "ADMIN");
        String token = jwtService.generateToken(user);

        assertThrows(JwtException.class, () -> jwtService.parseToken(token + "tampered"));
    }

    @Test
    void rejectsMissingOrWeakSecrets() {
        assertThrows(IllegalStateException.class, () -> new JwtService("", 60_000));
        assertThrows(IllegalStateException.class, () -> new JwtService("short-secret", 60_000));
    }
}
