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
@Table(name = "request_history")
public class RequestHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "request_type", nullable = false, length = 16)
    private RequestType requestType;
    @Column(name = "request_id", nullable = false)
    private Long requestId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_id", nullable = false)
    private User actor;
    @Enumerated(EnumType.STRING) @Column(name = "from_status", length = 32)
    private RequestStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name = "to_status", nullable = false, length = 32)
    private RequestStatus toStatus;
    @Column(columnDefinition = "TEXT")
    private String note;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public RequestHistory() {}
    public RequestHistory(RequestType type, Long requestId, User actor, RequestStatus from, RequestStatus to, String note) {
        this.requestType = type; this.requestId = requestId; this.actor = actor; this.fromStatus = from; this.toStatus = to; this.note = note;
    }
    public Long getId() { return id; }
    public RequestType getRequestType() { return requestType; }
    public Long getRequestId() { return requestId; }
    public User getActor() { return actor; }
    public RequestStatus getFromStatus() { return fromStatus; }
    public RequestStatus getToStatus() { return toStatus; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
