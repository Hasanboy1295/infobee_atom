package com.infobee.controller;

import com.infobee.dto.NotificationResponse;
import com.infobee.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "User notifications, unread count, mark as read")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "List notifications (paginated, optional unreadOnly filter)")
    public Page<NotificationResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Authentication auth) {
        return service.list(auth.getName(), page, size, unreadOnly);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get unread notification count")
    public Map<String, Object> summary(Authentication auth) {
        return service.summary(auth.getName());
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public void markRead(@PathVariable Long id, Authentication auth) {
        service.markRead(auth.getName(), id);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public Map<String, Integer> markAllRead(Authentication auth) {
        int count = service.markAllRead(auth.getName());
        return Map.of("markedCount", count);
    }
}
