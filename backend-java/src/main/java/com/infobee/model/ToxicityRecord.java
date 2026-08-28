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
@Table(name = "toxicity_records")
public class ToxicityRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "substance_name", nullable = false, length = 300)
    private String substanceName;

    @Column(name = "cas_number", length = 30)
    private String casNumber;

    @Column(name = "ec_number", length = 30)
    private String ecNumber;

    @Column(name = "source_db", nullable = false, length = 50)
    private String sourceDb; // PUBCHEM, ECHA, TOXCAST

    @Column(name = "source_id", length = 100)
    private String sourceId; // external DB ID

    @Column(name = "endpoint_name", nullable = false, length = 200)
    private String endpointName; // e.g. "Oral LD50", "Skin Irritation"

    @Column(name = "endpoint_value", columnDefinition = "TEXT")
    private String endpointValue;

    @Column(name = "endpoint_unit", length = 50)
    private String endpointUnit;

    @Column(name = "test_guideline", length = 200)
    private String testGuideline;

    @Column(name = "test_method", length = 200)
    private String testMethod;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public ToxicityRecord() {}

    public Long getId() { return id; }
    public String getSubstanceName() { return substanceName; }
    public void setSubstanceName(String substanceName) { this.substanceName = substanceName; }
    public String getCasNumber() { return casNumber; }
    public void setCasNumber(String casNumber) { this.casNumber = casNumber; }
    public String getEcNumber() { return ecNumber; }
    public void setEcNumber(String ecNumber) { this.ecNumber = ecNumber; }
    public String getSourceDb() { return sourceDb; }
    public void setSourceDb(String sourceDb) { this.sourceDb = sourceDb; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getEndpointName() { return endpointName; }
    public void setEndpointName(String endpointName) { this.endpointName = endpointName; }
    public String getEndpointValue() { return endpointValue; }
    public void setEndpointValue(String endpointValue) { this.endpointValue = endpointValue; }
    public String getEndpointUnit() { return endpointUnit; }
    public void setEndpointUnit(String endpointUnit) { this.endpointUnit = endpointUnit; }
    public String getTestGuideline() { return testGuideline; }
    public void setTestGuideline(String testGuideline) { this.testGuideline = testGuideline; }
    public String getTestMethod() { return testMethod; }
    public void setTestMethod(String testMethod) { this.testMethod = testMethod; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Instant getCreatedAt() { return createdAt; }
}
