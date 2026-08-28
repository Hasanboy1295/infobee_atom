package com.infobee.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "cpsr_requests")
public class CpsrRequest extends BaseRequest {

    @Column(name = "requester_name", length = 200)
    private String requesterName;

    @Column(name = "requester_email", length = 200)
    private String requesterEmail;

    @Column(name = "requester_phone", length = 50)
    private String requesterPhone;

    @Column(name = "company_name", length = 300)
    private String companyName;

    @Column(name = "product_name", length = 300)
    private String productName;

    @Column(name = "regulatory_framework", length = 200)
    private String regulatoryFramework; // EU SCCS, K-REACH, etc.

    @Column(name = "target_market", length = 200)
    private String targetMarket; // EU, Korea, Global

    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;

    public CpsrRequest() {}

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }
    public String getRequesterPhone() { return requesterPhone; }
    public void setRequesterPhone(String requesterPhone) { this.requesterPhone = requesterPhone; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getRegulatoryFramework() { return regulatoryFramework; }
    public void setRegulatoryFramework(String regulatoryFramework) { this.regulatoryFramework = regulatoryFramework; }
    public String getTargetMarket() { return targetMarket; }
    public void setTargetMarket(String targetMarket) { this.targetMarket = targetMarket; }
    public String getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }
}
