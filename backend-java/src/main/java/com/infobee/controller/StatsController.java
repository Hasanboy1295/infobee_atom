package com.infobee.controller;

import com.infobee.dto.DashboardStats;
import com.infobee.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@Tag(name = "Statistics", description = "Dashboard statistics")
@SecurityRequirement(name = "bearerAuth")
public class StatsController {
    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard stats", description = "Returns aggregated counts for users, requests, comments, and history. Requires authentication.")
    public DashboardStats dashboard() {
        return statsService.getDashboardStats();
    }
}
