package com.infobee.dto;

import com.infobee.model.ActivityLog;
import java.time.Instant;

public record ActivityLogResponse(
    Long id,
    Long actorId,
    String actorUsername,
    String action,
    String targetType,
    Long targetId,
    String detail,
    String ipAddress,
    Instant createdAt
) {
    public static ActivityLogResponse from(ActivityLog log) {
        return new ActivityLogResponse(
            log.getId(),
            log.getActor() != null ? log.getActor().getId() : null,
            log.getActor() != null ? log.getActor().getUsername() : null,
            log.getAction().name(),
            log.getTargetType() != null ? log.getTargetType().name() : null,
            log.getTargetId(),
            log.getDetail(),
            log.getIpAddress(),
            log.getCreatedAt()
        );
    }
}
