package com.infobee.dto;

import com.infobee.model.BaseRequest;
import com.infobee.model.CpsrRequest;
import com.infobee.model.RequestStatus;
import java.time.Instant;

public record RequestResponse(
    Long id,
    String type,
    Long ownerId,
    String ownerUsername,
    Long departmentId,
    String departmentName,
    String title,
    String description,
    RequestStatus status,
    String priority,
    Instant dueDate,
    String tags,
    String evaluationResult,
    Instant createdAt,
    Instant updatedAt,
    // CPSR-specific fields (null for ATOM requests)
    String requesterName,
    String requesterEmail,
    String requesterPhone,
    String companyName,
    String productName,
    String regulatoryFramework,
    String targetMarket,
    String additionalInfo
) {
    public static RequestResponse from(BaseRequest r, String type) {
        String rName = null, rEmail = null, rPhone = null, cName = null,
               pName = null, regFw = null, tMarket = null, addInfo = null;
        if (r instanceof CpsrRequest c) {
            rName = c.getRequesterName();
            rEmail = c.getRequesterEmail();
            rPhone = c.getRequesterPhone();
            cName = c.getCompanyName();
            pName = c.getProductName();
            regFw = c.getRegulatoryFramework();
            tMarket = c.getTargetMarket();
            addInfo = c.getAdditionalInfo();
        }
        return new RequestResponse(
            r.getId(),
            type,
            r.getOwner().getId(),
            r.getOwner().getUsername(),
            r.getDepartment() != null ? r.getDepartment().getId() : null,
            r.getDepartment() != null ? r.getDepartment().getName() : null,
            r.getTitle(),
            r.getDescription(),
            r.getStatus(),
            r.getPriority() != null ? r.getPriority().name() : null,
            r.getDueDate(),
            r.getTags(),
            r.getEvaluationResult(),
            r.getCreatedAt(),
            r.getUpdatedAt(),
            rName, rEmail, rPhone, cName, pName, regFw, tMarket, addInfo
        );
    }
}
