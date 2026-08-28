package com.infobee.dto;

import com.infobee.model.Notification;

public record NotificationResponse(
    Long id, String type, String title, String message,
    String entityType, Long entityId, boolean read, java.time.Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
            n.getEntityType(), n.getEntityId(), n.isRead(), n.getCreatedAt());
    }
}
