package com.infobee.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Service health")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Public liveness response.")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "atom-backend",
            "message", "Backend is running"
        );
    }
}
