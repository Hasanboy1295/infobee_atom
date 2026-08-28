package com.infobee.repository;

import com.infobee.model.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ActivityLog> findByActorIdOrderByCreatedAtDesc(Long actorId, Pageable pageable);
    Page<ActivityLog> findByActionOrderByCreatedAtDesc(ActivityLog.Action action, Pageable pageable);
    Page<ActivityLog> findByActorIdAndActionOrderByCreatedAtDesc(Long actorId, ActivityLog.Action action, Pageable pageable);
}
