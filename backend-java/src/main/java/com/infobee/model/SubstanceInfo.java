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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "substance_infos")
public class SubstanceInfo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cpsr_request_id", nullable = false)
    private CpsrRequest cpsrRequest;

    @Column(name = "substance_name", nullable = false, length = 300)
    private String substanceName;

    @Column(name = "cas_number", length = 30)
    private String casNumber;

    @Column(name = "ec_number", length = 30)
    private String ecNumber;

    @Column(name = "molecular_formula", length = 100)
    private String molecularFormula;

    @Column(name = "molecular_weight")
    private Double molecularWeight;

    @Column(length = 100)
    private String purity;

    @Column(name = "intended_use", columnDefinition = "TEXT")
    private String intendedUse;

    @Column(name = "intended_concentration")
    private Double intendedConcentration;

    @Column(name = "product_type", length = 100)
    private String productType;

    @Column(name = "target_population", length = 100)
    private String targetPopulation;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public SubstanceInfo() {}

    public Long getId() { return id; }
    public CpsrRequest getCpsrRequest() { return cpsrRequest; }
    public void setCpsrRequest(CpsrRequest cpsrRequest) { this.cpsrRequest = cpsrRequest; }
    public String getSubstanceName() { return substanceName; }
    public void setSubstanceName(String substanceName) { this.substanceName = substanceName; }
    public String getCasNumber() { return casNumber; }
    public void setCasNumber(String casNumber) { this.casNumber = casNumber; }
    public String getEcNumber() { return ecNumber; }
    public void setEcNumber(String ecNumber) { this.ecNumber = ecNumber; }
    public String getMolecularFormula() { return molecularFormula; }
    public void setMolecularFormula(String molecularFormula) { this.molecularFormula = molecularFormula; }
    public Double getMolecularWeight() { return molecularWeight; }
    public void setMolecularWeight(Double molecularWeight) { this.molecularWeight = molecularWeight; }
    public String getPurity() { return purity; }
    public void setPurity(String purity) { this.purity = purity; }
    public String getIntendedUse() { return intendedUse; }
    public void setIntendedUse(String intendedUse) { this.intendedUse = intendedUse; }
    public Double getIntendedConcentration() { return intendedConcentration; }
    public void setIntendedConcentration(Double intendedConcentration) { this.intendedConcentration = intendedConcentration; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public String getTargetPopulation() { return targetPopulation; }
    public void setTargetPopulation(String targetPopulation) { this.targetPopulation = targetPopulation; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
