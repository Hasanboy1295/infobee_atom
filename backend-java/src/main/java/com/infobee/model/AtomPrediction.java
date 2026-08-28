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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "atom_predictions")
public class AtomPrediction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atom_request_id", nullable = false)
    private AtomRequest atomRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PredictionStatus status = PredictionStatus.INPUT_READY;

    @Lob
    @Column(name = "input_conditions", nullable = false)
    private String inputConditions; // JSON: experiment parameters

    @Column(name = "input_filename", length = 300)
    private String inputFilename;

    @Column(name = "input_content_type", length = 100)
    private String inputContentType;

    @Lob
    @Column(name = "result_data")
    private String resultData; // JSON: prediction results

    @Column(name = "result_filename", length = 300)
    private String resultFilename;

    @Column(name = "result_content_type", length = 100)
    private String resultContentType;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public enum PredictionStatus {
        INPUT_READY, QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    public AtomPrediction() {}

    public Long getId() { return id; }
    public AtomRequest getAtomRequest() { return atomRequest; }
    public void setAtomRequest(AtomRequest atomRequest) { this.atomRequest = atomRequest; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public PredictionStatus getStatus() { return status; }
    public void setStatus(PredictionStatus status) { this.status = status; }
    public String getInputConditions() { return inputConditions; }
    public void setInputConditions(String inputConditions) { this.inputConditions = inputConditions; }
    public String getInputFilename() { return inputFilename; }
    public void setInputFilename(String inputFilename) { this.inputFilename = inputFilename; }
    public String getInputContentType() { return inputContentType; }
    public void setInputContentType(String inputContentType) { this.inputContentType = inputContentType; }
    public String getResultData() { return resultData; }
    public void setResultData(String resultData) { this.resultData = resultData; }
    public String getResultFilename() { return resultFilename; }
    public void setResultFilename(String resultFilename) { this.resultFilename = resultFilename; }
    public String getResultContentType() { return resultContentType; }
    public void setResultContentType(String resultContentType) { this.resultContentType = resultContentType; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public Long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
