package com.infobee.dto;

import java.util.Map;

public record DashboardStats(
    long totalUsers,
    long activeUsers,
    long totalDepartments,
    long totalRoles,
    long totalAtomRequests,
    long totalCpsrRequests,
    Map<String, Long> atomByStatus,
    Map<String, Long> cpsrByStatus,
    long totalComments,
    long totalHistoryEntries
) {
}
