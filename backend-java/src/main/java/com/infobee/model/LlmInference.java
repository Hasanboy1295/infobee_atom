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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "llm_inferences")
public class LlmInference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cpsr_request_id", nullable = false)
    private CpsrRequest cpsrRequest;

    @Column(name = "model_name", nullable = false, length = 200)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InferenceType inferenceType;

    @Lob
    @Column(nullable = false)
    private String prompt;

    @Lob
    @Column(name = "response_text")
    private String responseText;

    @Column(name = "reference_sources", columnDefinition = "TEXT")
    private String referenceSources; // JSON array of reference URLs/DOIs

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InferenceStatus status = InferenceStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public enum InferenceType {
        TOXICITY_ASSESSMENT, CPSR_GENERATION, SAFETY_REVIEW, REFERENCE_LOOKUP
    }

    public enum InferenceStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }

    public LlmInference() {}

    public Long getId() { return id; }
    public CpsrRequest getCpsrRequest() { return cpsrRequest; }
    public void setCpsrRequest(CpsrRequest cpsrRequest) { this.cpsrRequest = cpsrRequest; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public InferenceType getInferenceType() { return inferenceType; }
    public void setInferenceType(InferenceType inferenceType) { this.inferenceType = inferenceType; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getResponseText() { return responseText; }
    public void setResponseText(String responseText) { this.responseText = responseText; }
    public String getReferenceSources() { return referenceSources; }
    public void setReferenceSources(String referenceSources) { this.referenceSources = referenceSources; }
    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
    public Long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Long latencyMs) { this.latencyMs = latencyMs; }
    public InferenceStatus getStatus() { return status; }
    public void setStatus(InferenceStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
}
