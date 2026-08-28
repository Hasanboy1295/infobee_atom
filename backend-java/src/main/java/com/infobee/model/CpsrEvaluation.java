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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cpsr_evaluations")
public class CpsrEvaluation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cpsr_request_id", nullable = false)
    private CpsrRequest cpsrRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id")
    private User evaluator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EvaluationStatus status = EvaluationStatus.PENDING;

    @Column(name = "sed_value")
    private Double sedValue; // Systemic Exposure Dose

    @Column(name = "sed_unit", length = 50)
    private String sedUnit;

    @Column(name = "mos_value")
    private Double mosValue; // Margin of Safety

    @Column(name = "noael_value")
    private Double noaelValue;

    @Column(name = "noael_unit", length = 50)
    private String noaelUnit;

    @Column(name = "risk_assessment", columnDefinition = "TEXT")
    private String riskAssessment;

    @Column(name = "conclusion", columnDefinition = "TEXT")
    private String conclusion;

    @Column(name = "evaluator_opinion", columnDefinition = "TEXT")
    private String evaluatorOpinion;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public enum EvaluationStatus {
        PENDING, IN_PROGRESS, COMPLETED, APPROVED, REJECTED, CANCELLED
    }

    public CpsrEvaluation() {}

    public Long getId() { return id; }
    public CpsrRequest getCpsrRequest() { return cpsrRequest; }
    public void setCpsrRequest(CpsrRequest cpsrRequest) { this.cpsrRequest = cpsrRequest; }
    public User getEvaluator() { return evaluator; }
    public void setEvaluator(User evaluator) { this.evaluator = evaluator; }
    public EvaluationStatus getStatus() { return status; }
    public void setStatus(EvaluationStatus status) { this.status = status; }
    public Double getSedValue() { return sedValue; }
    public void setSedValue(Double sedValue) { this.sedValue = sedValue; }
    public String getSedUnit() { return sedUnit; }
    public void setSedUnit(String sedUnit) { this.sedUnit = sedUnit; }
    public Double getMosValue() { return mosValue; }
    public void setMosValue(Double mosValue) { this.mosValue = mosValue; }
    public Double getNoaelValue() { return noaelValue; }
    public void setNoaelValue(Double noaelValue) { this.noaelValue = noaelValue; }
    public String getNoaelUnit() { return noaelUnit; }
    public void setNoaelUnit(String noaelUnit) { this.noaelUnit = noaelUnit; }
    public String getRiskAssessment() { return riskAssessment; }
    public void setRiskAssessment(String riskAssessment) { this.riskAssessment = riskAssessment; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public String getEvaluatorOpinion() { return evaluatorOpinion; }
    public void setEvaluatorOpinion(String evaluatorOpinion) { this.evaluatorOpinion = evaluatorOpinion; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
