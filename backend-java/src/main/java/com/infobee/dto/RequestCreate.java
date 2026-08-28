package com.infobee.dto;

import jakarta.validation.constraints.Size;

public record RequestCreate(
    @jakarta.validation.constraints.NotBlank(message = "title is required")
    @Size(max = 200, message = "title must be at most 200 characters") String title,
    @jakarta.validation.constraints.NotBlank(message = "description is required") String description,
    Long departmentId,
    String priority,
    String dueDate,
    String tags,
    // CPSR-specific fields (ignored for ATOM requests)
    @Size(max = 200) String requesterName,
    @Size(max = 200) String requesterEmail,
    @Size(max = 50) String requesterPhone,
    @Size(max = 300) String companyName,
    @Size(max = 300) String productName,
    @Size(max = 200) String regulatoryFramework,
    @Size(max = 200) String targetMarket,
    String additionalInfo
) {}
