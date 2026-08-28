package com.infobee.error;

import java.time.Instant;
import java.util.Map;

public record ApiError(Instant timestamp, int status, String error, String message, Map<String, String> validationErrors) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, Map.of());
    }
}
