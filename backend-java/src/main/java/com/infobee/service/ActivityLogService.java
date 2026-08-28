package com.infobee.service;

import com.infobee.dto.ActivityLogResponse;
import com.infobee.dto.RequestPageResponse;
import com.infobee.model.ActivityLog;
import com.infobee.model.RequestType;
import com.infobee.model.User;
import com.infobee.repository.ActivityLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityLogService {
    private final ActivityLogRepository repository;

    public ActivityLogService(ActivityLogRepository repository) {
        this.repository = repository;
    }

    public void log(User actor, ActivityLog.Action action, RequestType targetType, Long targetId, String detail, String ipAddress) {
        repository.save(new ActivityLog(actor, action, targetType, targetId, detail, ipAddress));
    }

    public void logWithoutActor(ActivityLog.Action action, RequestType targetType, Long targetId, String detail, String ipAddress) {
        repository.save(new ActivityLog(null, action, targetType, targetId, detail, ipAddress));
    }

    @Transactional(readOnly = true)
    public RequestPageResponse<ActivityLogResponse> list(Pageable pageable) {
        Page<ActivityLog> page = repository.findAllByOrderByCreatedAtDesc(pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public RequestPageResponse<ActivityLogResponse> listByActor(Long actorId, Pageable pageable) {
        Page<ActivityLog> page = repository.findByActorIdOrderByCreatedAtDesc(actorId, pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public RequestPageResponse<ActivityLogResponse> listByAction(ActivityLog.Action action, Pageable pageable) {
        Page<ActivityLog> page = repository.findByActionOrderByCreatedAtDesc(action, pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public RequestPageResponse<ActivityLogResponse> listByActorAndAction(Long actorId, ActivityLog.Action action, Pageable pageable) {
        Page<ActivityLog> page = repository.findByActorIdAndActionOrderByCreatedAtDesc(actorId, action, pageable);
        return toPageResponse(page);
    }

    private RequestPageResponse<ActivityLogResponse> toPageResponse(Page<ActivityLog> page) {
        return new RequestPageResponse<>(
            page.getContent().stream().map(ActivityLogResponse::from).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
