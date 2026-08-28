package com.infobee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "request_comments")
public class RequestComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "atom_request_id")
    private AtomRequest atomRequest;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cpsr_request_id")
    private CpsrRequest cpsrRequest;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id", nullable = false)
    private User author;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }
    public RequestComment() {}
    public Long getId() { return id; }
    public AtomRequest getAtomRequest() { return atomRequest; }
    public void setAtomRequest(AtomRequest atomRequest) { this.atomRequest = atomRequest; }
    public CpsrRequest getCpsrRequest() { return cpsrRequest; }
    public void setCpsrRequest(CpsrRequest cpsrRequest) { this.cpsrRequest = cpsrRequest; }
    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getCreatedAt() { return createdAt; }
}
