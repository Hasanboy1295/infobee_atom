package com.infobee.controller;

import com.infobee.dto.ActivityLogResponse;
import com.infobee.dto.RequestPageResponse;
import com.infobee.model.ActivityLog;
import com.infobee.service.ActivityLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/activity-logs")
@Tag(name = "Activity logs", description = "Audit trail of all system actions")
@SecurityRequirement(name = "bearerAuth")
public class ActivityLogController {
    private final ActivityLogService service;

    public ActivityLogController(ActivityLogService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List activity logs", description = "ADMIN only. Returns all system activity with optional filters.")
    public RequestPageResponse<ActivityLogResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(required = false) Long actorId,
        @RequestParam(required = false) String action,
        Authentication auth
    ) {
        if (!isAdmin(auth)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, 200), Sort.by(Sort.Direction.DESC, "createdAt"));

        if (actorId != null && action != null) {
            return service.listByActorAndAction(actorId, parseAction(action), pageable);
        } else if (actorId != null) {
            return service.listByActor(actorId, pageable);
        } else if (action != null) {
            return service.listByAction(parseAction(action), pageable);
        }
        return service.list(pageable);
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private ActivityLog.Action parseAction(String action) {
        try {
            return ActivityLog.Action.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action value: " + action);
        }
    }
}
