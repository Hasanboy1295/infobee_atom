package com.infobee.service;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {
    private final int maxAttempts;
    private final long lockTimeMs;
    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
        @Value("${app.security.login.max-attempts:10}") int maxAttempts,
        @Value("${app.security.login.lock-time-ms:900000}") long lockTimeMs
    ) {
        this.maxAttempts = maxAttempts;
        this.lockTimeMs = lockTimeMs;
    }

    public boolean isBlocked(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null) return false;
        if (info.lockedUntil > System.currentTimeMillis()) return true;
        attempts.remove(key);
        return false;
    }

    public void recordFailedAttempt(String key) {
        attempts.compute(key, (k, existing) -> {
            if (existing == null || existing.lockedUntil <= System.currentTimeMillis()) {
                return new AttemptInfo(1, System.currentTimeMillis() + lockTimeMs);
            }
            int count = existing.count + 1;
            long lockUntil = count >= maxAttempts ? System.currentTimeMillis() + lockTimeMs : existing.lockedUntil;
            return new AttemptInfo(count, lockUntil);
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    public void resetAll() {
        attempts.clear();
    }

    public int getFailedAttempts(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null || info.lockedUntil <= System.currentTimeMillis()) return 0;
        return info.count;
    }

    private static class AttemptInfo {
        final int count;
        final long lockedUntil;

        AttemptInfo(int count, long lockedUntil) {
            this.count = count;
            this.lockedUntil = lockedUntil;
        }
    }
}
