package com.infobee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private Action action;

    @Enumerated(EnumType.STRING) @Column(length = 16)
    private RequestType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(length = 255)
    private String detail;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public ActivityLog() {}

    public ActivityLog(User actor, Action action, RequestType targetType, Long targetId, String detail, String ipAddress) {
        this.actor = actor;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
        this.ipAddress = ipAddress;
    }

    public enum Action {
        LOGIN, LOGOUT, SIGNUP,
        REQUEST_CREATED, REQUEST_UPDATED, REQUEST_DELETED, REQUEST_TRANSITIONED,
        COMMENT_ADDED, FILE_UPLOADED, FILE_DELETED,
        USER_CREATED, USER_UPDATED, USER_ENABLED, USER_DISABLED,
        LOGIN_BLOCKED
    }

    public Long getId() { return id; }
    public User getActor() { return actor; }
    public Action getAction() { return action; }
    public RequestType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getDetail() { return detail; }
    public String getIpAddress() { return ipAddress; }
    public Instant getCreatedAt() { return createdAt; }
}
